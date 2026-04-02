module Ledger.BalanceSpec (spec) where

import Test.Hspec

import Ledger.Types
import Ledger.Balance

-- Tests derived from src/test/java/ledger/model/BalanceTest.java

spec :: Spec
spec = describe "Balance" $ do

  describe "addBalance" $ do
    it "adds two balances component-wise" $ do
      let b1 = Balance 100 100 100 100
          b2 = Balance 200 200 200 200
          result = addBalance b1 b2
      result `shouldBe` Balance 300 300 300 300

  describe "subtractBalance" $ do
    it "subtracts two balances component-wise" $ do
      let b1 = Balance 200 200 200 200
          b2 = Balance 100 100 100 100
          result = subtractBalance b1 b2
      result `shouldBe` Balance 100 100 100 100

  describe "equality" $ do
    it "returns True when two balances are equal" $ do
      let b = Balance 100 100 100 100
      b `shouldBe` b

    it "returns False when balances differ" $ do
      let b1 = Balance 100 100 100 100
          b2 = Balance 200 200 200 200
      b1 `shouldNotBe` b2

  describe "negateBalance" $ do
    it "negates all components" $ do
      let b = Balance 100 50 25 10
      negateBalance b `shouldBe` Balance (-100) (-50) (-25) (-10)

  describe "totalAmount" $ do
    it "sums all four components" $ do
      totalAmount (Balance 100 50 25 10) `shouldBe` 185

  describe "getComponent / setComponent" $ do
    it "gets and sets principal" $ do
      let b = Balance 100 50 25 10
      getComponent Principal b `shouldBe` 100
      setComponent Principal 999 b `shouldBe` Balance 999 50 25 10

    it "gets and sets interest" $ do
      let b = Balance 100 50 25 10
      getComponent Interest b `shouldBe` 50
      setComponent Interest 999 b `shouldBe` Balance 100 999 25 10

    it "gets and sets fees" $ do
      let b = Balance 100 50 25 10
      getComponent Fees b `shouldBe` 25
      setComponent Fees 999 b `shouldBe` Balance 100 50 999 10

    it "gets and sets excess" $ do
      let b = Balance 100 50 25 10
      getComponent Excess b `shouldBe` 10
      setComponent Excess 999 b `shouldBe` Balance 100 50 25 999
