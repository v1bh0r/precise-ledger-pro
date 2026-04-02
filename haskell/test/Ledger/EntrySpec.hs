module Ledger.EntrySpec (spec) where

import Test.Hspec
import Data.Time (fromGregorian, midnight)

import Ledger.Types
import Ledger.Entry

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

  describe "idempotencyKey" $ do
    it "produces a deterministic key" $ do
      let key = idempotencyKey sampleEntry
      key `shouldNotBe` ""
      -- Same entry always produces same key
      idempotencyKey sampleEntry `shouldBe` key
