module Ledger.CoreSpec (spec) where

import Test.Hspec
import Data.Time (fromGregorian, midnight)

import Ledger.Types
import Ledger.Core


spec :: Spec
spec = describe "Ledger.Core" $ do

  let t1 = LocalTime (fromGregorian 2024 1 1) midnight
      t2 = LocalTime (fromGregorian 2024 2 1) midnight
      t3 = LocalTime (fromGregorian 2024 3 1) midnight
      loanId = "loan-1"
      emptyLedger = Ledger loanId zeroBalance [] "USD"

      mkEntry eid eType p i f e effAt creAt aType aId = LedgerEntry
        { leLoanId = loanId, leEntryId = eid, leEntryType = eType
        , leAmount = p + i + f + e
        , lePrincipal = p, leInterest = i, leFee = f, leExcess = e
        , lePrincipalBalance = p, leInterestBalance = i
        , leFeeBalance = f, leExcessBalance = e
        , leEffectiveAt = effAt, leCreatedAt = creAt
        , leSourceLedgerActivityType = aType
        , leSourceLedgerActivityId = aId
        }

  describe "currentBalance" $ do
    it "returns start balance for empty ledger" $
      currentBalance emptyLedger `shouldBe` zeroBalance

    it "returns balance of last entry" $ do
      let e = mkEntry "e1" "Disbursal" 1000 0 0 0 t1 t1 "Transaction" "1"
          ledger = addEntry e emptyLedger
      currentBalance ledger `shouldBe` Balance 1000 0 0 0

  describe "addEntry" $ do
    it "appends entry with recomputed balances" $ do
      let e = mkEntry "e1" "Disbursal" 1000 0 0 0 t1 t1 "Transaction" "1"
          ledger = addEntry e emptyLedger
      length (ledgerEntries ledger) `shouldBe` 1
      currentBalance ledger `shouldBe` Balance 1000 0 0 0

    it "is idempotent — duplicate entries are not added" $ do
      let e = mkEntry "e1" "Disbursal" 1000 0 0 0 t1 t1 "Transaction" "1"
          ledger  = addEntry e emptyLedger
          ledger2 = addEntry e ledger
      length (ledgerEntries ledger2) `shouldBe` 1

    it "stacks balances correctly across multiple entries" $ do
      let e1 = mkEntry "e1" "Disbursal"  1000000 0 0 0 t1 t1 "Transaction" "1"
          e2 = mkEntry "e2" "Payment"   (-10000) 0 0 0 t2 t2 "Transaction" "2"
          ledger = addEntry e2 (addEntry e1 emptyLedger)
      currentBalance ledger `shouldBe` Balance 990000 0 0 0

  describe "rollbackToEntryBefore" $ do
    it "keeps entries effective on or before the given date" $ do
      let e1 = mkEntry "e1" "D" 1000 0 0 0 t1 t1 "T" "1"
          e2 = mkEntry "e2" "D" 500  0 0 0 t2 t2 "T" "2"
          e3 = mkEntry "e3" "D" 200  0 0 0 t3 t3 "T" "3"
          ledger = foldl (flip addEntry) emptyLedger [e1, e2, e3]
          rolled = rollbackToEntryBefore t2 ledger
      length (ledgerEntries rolled) `shouldBe` 2

  describe "rollbackToEntryBeforeActivity" $ do
    it "keeps entries before the identified activity" $ do
      let e1 = mkEntry "e1" "D" 1000 0 0 0 t1 t1 "Transaction" "1"
          e2 = mkEntry "e2" "D" 500  0 0 0 t2 t2 "Transaction" "2"
          e3 = mkEntry "e3" "D" 200  0 0 0 t3 t3 "Transaction" "3"
          ledger = foldl (flip addEntry) emptyLedger [e1, e2, e3]
          rolled = rollbackToEntryBeforeActivity "Transaction" "2" ledger
      length (ledgerEntries rolled) `shouldBe` 1

  describe "calculateTotalImpact" $ do
    it "sums all entries from the same activity" $ do
      let e1 = mkEntry "e1" "D" 500 10 0 0 t1 t1 "Transaction" "1"
          e2 = mkEntry "e2" "D" 500 10 0 0 t2 t2 "Transaction" "1"
          ledger = foldl (flip addEntry) emptyLedger [e1, e2]
      calculateTotalImpact "Transaction" "1" ledger
        `shouldBe` Just (Balance 1000 20 0 0)

    it "returns Nothing for unknown activity" $
      calculateTotalImpact "Unknown" "99" emptyLedger `shouldBe` Nothing

  describe "isBackdated" $ do
    it "returns False for empty ledger" $ do
      let act = StartOfDayActivity loanId "SOD" "StartOfDay" "sod-1" t1 t1
      isBackdated act emptyLedger `shouldBe` False

    it "returns True when activity is before last entry" $ do
      let e = mkEntry "e1" "D" 1000 0 0 0 t2 t2 "T" "1"
          ledger = addEntry e emptyLedger
          act = StartOfDayActivity loanId "SOD" "StartOfDay" "sod-1" t1 t1
      isBackdated act ledger `shouldBe` True

    it "returns False when activity is after last entry" $ do
      let e = mkEntry "e1" "D" 1000 0 0 0 t1 t1 "T" "1"
          ledger = addEntry e emptyLedger
          act = StartOfDayActivity loanId "SOD" "StartOfDay" "sod-1" t3 t3
      isBackdated act ledger `shouldBe` False

  describe "balanceAt" $ do
    it "returns start balance before any entry" $ do
      let e = mkEntry "e1" "D" 1000 0 0 0 t2 t2 "T" "1"
          ledger = addEntry e emptyLedger
      balanceAt t1 ledger `shouldBe` zeroBalance

    it "returns balance at a specific time" $ do
      let e1 = mkEntry "e1" "D" 1000 0 0 0 t1 t1 "T" "1"
          e2 = mkEntry "e2" "D" 500  0 0 0 t3 t3 "T" "2"
          ledger = foldl (flip addEntry) emptyLedger [e1, e2]
      balanceAt t2 ledger `shouldBe` Balance 1000 0 0 0
