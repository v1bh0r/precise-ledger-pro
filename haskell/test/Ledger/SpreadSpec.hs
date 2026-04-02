module Ledger.SpreadSpec (spec) where

import Test.Hspec

import Ledger.Types
import Ledger.Spread

spec :: Spec
spec = describe "Ledger.Spread" $ do

  describe "parseSpreadConfig" $ do
    it "parses FIP" $
      parseSpreadConfig "FIP" `shouldBe` [Fees, Interest, Principal]

    it "parses P" $
      parseSpreadConfig "P" `shouldBe` [Principal]

    it "parses empty string" $
      parseSpreadConfig "" `shouldBe` []

  describe "applyStaticSpread" $ do
    it "adds allocation on Credit" $ do
      let bal   = Balance 1000 100 50 0
          alloc = Balance 500  50  25 0
      applyStaticSpread Credit bal alloc
        `shouldBe` Balance 1500 150 75 0

    it "subtracts allocation on Debit" $ do
      let bal   = Balance 1000 100 50 0
          alloc = Balance 500  50  25 0
      applyStaticSpread Debit bal alloc
        `shouldBe` Balance 500 50 25 0

  describe "applyComputationalSpread — Credit" $ do
    it "credits principal with spread P" $ do
      let bal = Balance 0 0 0 0
      applyComputationalSpread Credit [Principal] 1000000 bal
        `shouldBe` Balance 1000000 0 0 0

    it "reduces excess first, then credits first component" $ do
      let bal = Balance 0 0 0 500
      applyComputationalSpread Credit [Principal] 1000 bal
        `shouldBe` Balance 500 0 0 0

    it "absorbs full amount in excess if excess >= amount" $ do
      let bal = Balance 0 0 0 2000
      applyComputationalSpread Credit [Principal] 1000 bal
        `shouldBe` Balance 0 0 0 1000

  describe "applyComputationalSpread — Debit" $ do
    it "reduces fees first, then interest, then principal" $ do
      let bal = Balance 1000 200 50 0
      applyComputationalSpread Debit defaultSpreadConfig 300 bal
        `shouldBe` Balance 950 0 0 0

    it "overflows to excess when all components exhausted" $ do
      let bal = Balance 100 50 25 0
      applyComputationalSpread Debit defaultSpreadConfig 200 bal
        `shouldBe` Balance 0 0 0 25

    it "handles exact match" $ do
      let bal = Balance 100 50 25 0
      applyComputationalSpread Debit defaultSpreadConfig 175 bal
        `shouldBe` Balance 0 0 0 0

    it "applies debit with spread P only" $ do
      let bal = Balance 1000000 0 0 0
      applyComputationalSpread Debit [Principal] 10000 bal
        `shouldBe` Balance 990000 0 0 0
