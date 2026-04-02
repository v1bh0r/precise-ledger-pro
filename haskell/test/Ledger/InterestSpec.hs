module Ledger.InterestSpec (spec) where

import Test.Hspec
import Data.Time (fromGregorian, midnight)

import Ledger.Types
import Ledger.Interest

spec :: Spec
spec = describe "Ledger.Interest" $ do

  describe "calculateDailyInterest" $ do
    it "computes principal * rate / daysInYear" $ do
      -- 1,000,000 * 0.1 / 365 ≈ 273.97
      let result = calculateDailyInterest 1000000 0.1 365
      abs (result - 273.9726027397) < 0.01 `shouldBe` True

    it "returns 0 for zero principal" $
      calculateDailyInterest 0 0.1 365 `shouldBe` 0

    it "returns 0 for zero rate" $
      calculateDailyInterest 1000000 0 365 `shouldBe` 0

  describe "getApplicableRate" $ do
    let t1 = LocalTime (fromGregorian 2024 1 1) midnight
        t2 = LocalTime (fromGregorian 2024 6 1) midnight
        t3 = LocalTime (fromGregorian 2024 12 1) midnight
        rates =
          [ InterestRate "loan1" 0.10 t1
          , InterestRate "loan1" 0.12 t2
          ]

    it "returns the first matching rate before effectiveAt" $
      getApplicableRate rates t3 `shouldBe` 0.10

    it "returns 0 when no rate is effective before the date" $
      getApplicableRate rates t1 `shouldBe` 0.0

    it "returns 0 for empty rates" $
      getApplicableRate [] t3 `shouldBe` 0.0
