module Ledger.ServiceSpec (spec) where

import Test.Hspec
import Data.Time (fromGregorian, midnight)

import Ledger.Types
import Ledger.Balance   (totalAmount)
import Ledger.Core      (addEntry, calculateTotalImpact, currentBalance)
import Ledger.Activity  (TemporalContext(..))
import Ledger.Service

-- Test fixtures derived from the Java LedgerServiceTest.

spec :: Spec
spec = describe "Ledger.Service" $ do

  let loanId  = "loan-1"
      t0      = LocalTime (fromGregorian 2024 1 1) midnight
      t1      = LocalTime (fromGregorian 2024 3 1) midnight
      t2      = LocalTime (fromGregorian 2024 3 2) midnight
      t3      = LocalTime (fromGregorian 2024 3 3) midnight
      t4      = LocalTime (fromGregorian 2024 3 4) midnight
      emptyLedger = Ledger loanId zeroBalance [] "USD"

      -- No-op lookup: no activities returned for backdated/reversal replay.
      -- Suitable for tests that don't trigger recursive replay.
      noLookup = ActivityLookup
        { lookupActivitiesAfter       = \_ _ _ -> []
        , lookupActivitiesCreatedSince = \_ _ _ _ -> []
        }

  ---------------------------------------------------------------------------
  -- applyLedgerActivities — basic
  ---------------------------------------------------------------------------
  describe "applyLedgerActivities — disbursal" $ do
    it "applies a single disbursal correctly" $ do
      let act = TransactionActivity loanId "Disbursal" "Transaction" "1"
                  t1 t1 1000000 Credit "P"
          (ledger, _) = applyLedgerActivities noLookup Nothing [act]
                          emptyLedger defaultClock
      currentBalance ledger `shouldBe` Balance 1000000 0 0 0

  describe "applyLedgerActivities — disbursal + payment" $ do
    it "reduces principal after payment" $ do
      let disbursal = TransactionActivity loanId "Disbursal" "Transaction" "1"
                        t1 t1 1000000 Credit "P"
          payment   = TransactionActivity loanId "Payment" "Transaction" "2"
                        t2 t2 10000 Debit "P"
          (ledger, _) = applyLedgerActivities noLookup Nothing
                          [disbursal, payment] emptyLedger defaultClock
      balPrincipal (currentBalance ledger) `shouldBe` 990000

  ---------------------------------------------------------------------------
  -- applyLedgerActivities — with interest accrual (StartOfDay)
  ---------------------------------------------------------------------------
  describe "applyLedgerActivities — interest accrual" $ do
    it "accrues daily interest on principal" $ do
      let ctx = Just $ TemporalContext
            { tcInterestRates = [InterestRate loanId 0.1 t0]
            , tcDaysInYear    = 365
            , tcCurrencyCode  = "USD"
            }
          disbursal = TransactionActivity loanId "Disbursal" "Transaction" "1"
                        t1 t1 1000000 Credit "P"
          sod       = StartOfDayActivity loanId "SOD" "StartOfDay"
                        (show t2) t2 t2
          (ledger, _) = applyLedgerActivities noLookup ctx
                          [disbursal, sod] emptyLedger defaultClock
          bal = currentBalance ledger
      -- B2 fix: interest is rounded to 2 dp == 273.97 exactly
      balInterest bal `shouldBe` 273.97
      balPrincipal bal `shouldBe` 1000000

  ---------------------------------------------------------------------------
  -- applyLedgerActivities — two-day interest accumulation
  ---------------------------------------------------------------------------
  describe "applyLedgerActivities — two-day interest" $ do
    it "accumulates rounded interest over two SOD activities" $ do
      let ctx = Just $ TemporalContext
            { tcInterestRates = [InterestRate loanId 0.1 t0]
            , tcDaysInYear    = 365
            , tcCurrencyCode  = "USD"
            }
          disbursal = TransactionActivity loanId "Disbursal" "Transaction" "1"
                        t1 t1 1000000 Credit "P"
          sod1      = StartOfDayActivity loanId "SOD" "StartOfDay"
                        (show t2) t2 t2
          sod2      = StartOfDayActivity loanId "SOD" "StartOfDay"
                        (show t4) t4 t4
          (ledger, _) = applyLedgerActivities noLookup ctx
                          [disbursal, sod1, sod2] emptyLedger defaultClock
          interestBal = balInterest (currentBalance ledger)
      -- Each SOD accrues 273.97; two days = 547.94
      abs (interestBal - 547.94) < 0.02 `shouldBe` True

  ---------------------------------------------------------------------------
  -- B1: reversal dispatched via applyLedgerActivities pipeline
  ---------------------------------------------------------------------------
  describe "applyLedgerActivities — reversal pipeline (B1 fix)" $ do
    it "reversal of a payment restores principal" $ do
      let disbursal = TransactionActivity loanId "Disbursal" "Transaction" "1"
                        t1 t1 1000000 Credit "P"
          payment   = TransactionActivity loanId "Payment"   "Transaction" "2"
                        t2 t2   10000 Debit  "P"
          reversal  = ReversalActivityData loanId "Reversal" "Reversal" "rev1"
                        t3 t3 "Transaction" "2"
          -- For retro replay after the payment, return just the disbursal
          lkup = noLookup
            { lookupActivitiesCreatedSince = \_ _ _ _ -> [disbursal] }
          (ledger, _) = applyLedgerActivities lkup Nothing
                          [disbursal, payment, reversal] emptyLedger defaultClock
      -- Compensation negates 10000 debit → principal back to 1,000,000
      balPrincipal (currentBalance ledger) `shouldBe` 1000000

    it "silently ignores a reversal of an unknown activity" $ do
      let reversal = ReversalActivityData loanId "Reversal" "Reversal" "rev1"
                       t3 t3 "Transaction" "no-such-id"
          (ledger, _) = applyLedgerActivities noLookup Nothing
                          [reversal] emptyLedger defaultClock
      -- ledger must be unchanged — no crash, no phantom entries
      currentBalance ledger `shouldBe` zeroBalance

  ---------------------------------------------------------------------------
  -- syncWithRetroactiveLedger
  ---------------------------------------------------------------------------
  describe "syncWithRetroactiveLedger" $ do

    let mkEntry eid eType p i f e effAt creAt aType aId = LedgerEntry
          { leLoanId = loanId, leEntryId = eid, leEntryType = eType
          , leAmount = p + i + f + e
          , lePrincipal = p, leInterest = i, leFee = f, leExcess = e
          , lePrincipalBalance = p, leInterestBalance = i
          , leFeeBalance = f, leExcessBalance = e
          , leEffectiveAt = effAt, leCreatedAt = creAt
          , leSourceLedgerActivityType = aType
          , leSourceLedgerActivityId = aId
          }

    it "adds new entries from retro ledger that are not in primary" $ do
      let primary = addEntry
            (mkEntry "e1" "D" 1000 0 0 0 t1 t1 "Transaction" "1")
            emptyLedger
          retroEntry = mkEntry "e2" "D" 500 0 0 0 t2 t2 "Transaction" "2"
          retro = foldl (flip addEntry) emptyLedger
            [ mkEntry "e1" "D" 1000 0 0 0 t1 t1 "Transaction" "1"
            , retroEntry
            ]
          result = syncWithRetroactiveLedger primary retro t3 t3 1
      length (ledgerEntries result) `shouldBe` 2

    it "creates adjustment entries when impacts differ" $ do
      let primary = addEntry
            (mkEntry "e1" "D" 1000 0 0 0 t1 t1 "Transaction" "1")
            emptyLedger
          retro = addEntry
            (mkEntry "e1" "D" 1200 0 0 0 t1 t1 "Transaction" "1")
            emptyLedger
          result = syncWithRetroactiveLedger primary retro t3 t3 0
      -- Should have original + adjustment = 2 entries
      length (ledgerEntries result) `shouldBe` 2
      -- Adjustment should be +200 principal
      case calculateTotalImpact "Transaction" "1" result of
        Just impact -> balPrincipal impact `shouldBe` 1200
        Nothing     -> expectationFailure "Expected impact"

    it "does nothing when impacts match" $ do
      let e = mkEntry "e1" "D" 1000 0 0 0 t1 t1 "Transaction" "1"
          primary = addEntry e emptyLedger
          retro   = addEntry e emptyLedger
          result  = syncWithRetroactiveLedger primary retro t3 t3 0
      length (ledgerEntries result) `shouldBe` 1

    it "returns primary unchanged if retroactive is empty" $ do
      let primary = addEntry
            (mkEntry "e1" "D" 1000 0 0 0 t1 t1 "Transaction" "1")
            emptyLedger
          retro = emptyLedger
          result = syncWithRetroactiveLedger primary retro t3 t3 0
      result `shouldBe` primary

  ---------------------------------------------------------------------------
  -- reverseLedgerActivity
  ---------------------------------------------------------------------------
  describe "reverseLedgerActivity" $ do

    it "returns Left when reversed activity has no entries" $ do
      let reversal = ReversalActivityData loanId "Reversal" "Reversal" "rev1"
                       t3 t3 "Transaction" "99"
          result = reverseLedgerActivity noLookup Nothing reversal
                     emptyLedger defaultClock
      result `shouldSatisfy` isLeft

    it "creates a compensation entry that negates the original impact" $ do
      let disbursal = TransactionActivity loanId "Disbursal" "Transaction" "1"
                        t1 t1 1000000 Credit "P"
          payment   = TransactionActivity loanId "Payment" "Transaction" "2"
                        t2 t2 10000 Debit "P"
          (ledger, clock) = applyLedgerActivities noLookup Nothing
                              [disbursal, payment] emptyLedger defaultClock

          -- Create a lookup that returns the disbursal when asked for
          -- activities after the reversed payment
          lkup = noLookup
            { lookupActivitiesCreatedSince = \_ _ _ _ -> [disbursal] }

          reversal = ReversalActivityData loanId "Reversal" "Reversal" "rev1"
                       t3 t3 "Transaction" "2"
          result = reverseLedgerActivity lkup Nothing reversal ledger clock

      case result of
        Left err -> expectationFailure $ "Expected Right, got: " ++ err
        Right (ledger', _) -> do
          -- The payment of 10000 should be reversed (P goes back to 1000000)
          -- After compensation + sync
          let paymentImpact = calculateTotalImpact "Transaction" "2" ledger'
          case paymentImpact of
            Just impact ->
              -- Compensation negates original, net impact should be zero
              totalAmount impact `shouldBe` 0
            Nothing -> expectationFailure "Expected payment impact entries"

  ---------------------------------------------------------------------------
  -- Edge case: testPastDatedPayment (from Java tests)
  ---------------------------------------------------------------------------
  describe "backdated payment" $ do
    it "handles a payment effective before the last entry" $ do
      -- Disbursal at t2, then a payment backdated to t1
      let disbursal = TransactionActivity loanId "Disbursal" "Transaction" "1"
                        t2 t2 1000000 Credit "P"
          backdatedPay = TransactionActivity loanId "Payment" "Transaction" "2"
                           t1 t3 20000 Debit "P"  -- effective t1, created t3

          -- Lookup returns the disbursal for replay after the backdated date
          lkup = noLookup
            { lookupActivitiesAfter = \_ _ _ -> [disbursal] }

          (ledger, _) = applyLedgerActivities lkup Nothing
                          [disbursal, backdatedPay] emptyLedger defaultClock
      -- Net: 1,000,000 disbursal - 20,000 payment = 980,000
      totalAmount (currentBalance ledger) `shouldBe` 980000

isLeft :: Either a b -> Bool
isLeft (Left _) = True
isLeft _        = False
