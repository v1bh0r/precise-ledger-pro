module Ledger.EntrySpec (spec) where

import Test.Hspec
import Data.Time (fromGregorian, midnight)

import Ledger.Types
import Ledger.Entry
import Ledger.Core (addEntry)

spec :: Spec
spec = describe "LedgerEntry" $ do

  let t1 = LocalTime (fromGregorian 2022 1 1) midnight
      sampleEntry = LedgerEntry
        { leLoanId                   = "1234"
        , leEntryId                  = "e1"
        , leEntryType                = "Test Type"
        , leAmount                   = 100
        , lePrincipal                = 100
        , leInterest                 = 10
        , leFee                      = 5
        , leExcess                   = 0
        , lePrincipalBalance         = 100
        , leInterestBalance          = 10
        , leFeeBalance               = 5
        , leExcessBalance            = 0
        , leEffectiveAt              = t1
        , leCreatedAt                = t1
        , leSourceLedgerActivityType = "Test Activity Type"
        , leSourceLedgerActivityId   = "9012"
        }

  describe "entryBalance" $ do
    it "extracts the running balance" $ do
      entryBalance sampleEntry `shouldBe` Balance 100 10 5 0

  describe "entryBalanceChange" $ do
    it "extracts the change (delta)" $ do
      entryBalanceChange sampleEntry `shouldBe` Balance 100 10 5 0

  describe "updateEntryBalances" $ do
    it "recomputes running balances from current ledger balance" $ do
      let current = Balance 500 50 20 0
          updated = updateEntryBalances current sampleEntry
      lePrincipalBalance updated `shouldBe` 600
      leInterestBalance  updated `shouldBe` 60
      leFeeBalance       updated `shouldBe` 25
      leExcessBalance    updated `shouldBe` 0

    -- B2: balances must be rounded to 2 dp after accumulation
    it "rounds balance fields to 2 decimal places" $ do
      let fractionalEntry = sampleEntry
            { lePrincipal = 1000.005
            , leInterest  = 0.005
            , leFee       = 0.0049
            , leExcess    = 0.0
            }
          current = Balance 0 0 0 0
          updated = updateEntryBalances current fractionalEntry
      lePrincipalBalance updated `shouldBe` 1000.01
      leInterestBalance  updated `shouldBe` 0.01
      leFeeBalance       updated `shouldBe` 0.0
      leExcessBalance    updated `shouldBe` 0.0

  describe "idempotencyKey" $ do
    it "produces a deterministic key" $ do
      let key = idempotencyKey sampleEntry
      key `shouldNotBe` ""
      -- Same entry always produces same key
      idempotencyKey sampleEntry `shouldBe` key

    -- B3: leEntryId must not be part of the key so semantic duplicates are caught
    it "is independent of leEntryId (B3)" $ do
      let e2 = sampleEntry { leEntryId = "different-id" }
      idempotencyKey sampleEntry `shouldBe` idempotencyKey e2

    it "differs when entryType changes" $ do
      let e2 = sampleEntry { leEntryType = "Other Type" }
      idempotencyKey sampleEntry `shouldNotBe` idempotencyKey e2

    it "differs when activityId changes" $ do
      let e2 = sampleEntry { leSourceLedgerActivityId = "9999" }
      idempotencyKey sampleEntry `shouldNotBe` idempotencyKey e2

  -- B3: addEntry must treat entries with different leEntryId but identical
  -- semantic fields as duplicates and not insert twice.
  describe "addEntry idempotency (B3)" $ do
    it "deduplicates entries with same semantic key but different leEntryId" $ do
      let emptyL = Ledger { ledgerLoanId = "1234", ledgerStartBalance = zeroBalance
                          , ledgerEntries = [], ledgerCurrency = "USD" }
          e2     = sampleEntry { leEntryId = "e2-duplicate" }
          ledger = addEntry e2 (addEntry sampleEntry emptyL)
      length (ledgerEntries ledger) `shouldBe` 1
