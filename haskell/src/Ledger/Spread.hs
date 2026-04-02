-- | Transaction spread strategies — how a transaction amount is allocated
-- across the four balance components.
--
-- Pure translations of @ComputationalSpread@ and @StaticSpread@ from Java.
module Ledger.Spread
  ( SpreadConfig
  , defaultSpreadConfig
  , parseSpreadConfig
  , applyStaticSpread
  , applyComputationalSpread
  ) where

import Data.List (foldl')

import Ledger.Types
import Ledger.Balance (getComponent, setComponent)

-- | Ordered list of balance components to spread into.
type SpreadConfig = [BalanceComponent]

-- | Default spread order: Fees -> Interest -> Principal.
defaultSpreadConfig :: SpreadConfig
defaultSpreadConfig = [Fees, Interest, Principal]

-- | Parse a single-character-per-component spread string.
--
-- >>> parseSpreadConfig "FIP"
-- [Fees, Interest, Principal]
-- >>> parseSpreadConfig "P"
-- [Principal]
parseSpreadConfig :: String -> SpreadConfig
parseSpreadConfig = concatMap charToComponent
  where
    charToComponent 'F' = [Fees]
    charToComponent 'I' = [Interest]
    charToComponent 'P' = [Principal]
    charToComponent 'E' = [Excess]
    charToComponent _   = []

-- | Static spread: add or subtract a fixed balance allocation.
applyStaticSpread :: Direction -> Balance -> Balance -> Balance
applyStaticSpread Credit bal alloc = Balance
  { balPrincipal = balPrincipal bal + balPrincipal alloc
  , balInterest  = balInterest  bal + balInterest  alloc
  , balFee       = balFee       bal + balFee       alloc
  , balExcess    = balExcess    bal + balExcess    alloc
  }
applyStaticSpread Debit bal alloc = Balance
  { balPrincipal = balPrincipal bal - balPrincipal alloc
  , balInterest  = balInterest  bal - balInterest  alloc
  , balFee       = balFee       bal - balFee       alloc
  , balExcess    = balExcess    bal - balExcess    alloc
  }

-- | Computational spread: allocate a transaction amount across components
-- in the configured order.
--
-- **Credit path** (Java @applyCreditOperation@):
--   1. Reduce excess balance first (if any), absorbing as much of the
--      credit amount as the excess allows.
--   2. Add the remaining amount to the first component in the spread config.
--      (Java sets @remainingAmount = 0@ after each loop iteration, so only
--      the first component receives the amount.)
--
-- **Debit path** (Java @applyDebitOperation@):
--   1. Walk the spread config in order, reducing each component toward zero.
--   2. Any leftover (amount not absorbed) accumulates into excess.
applyComputationalSpread :: Direction -> SpreadConfig -> Money -> Balance -> Balance
applyComputationalSpread Credit config amount bal =
  let (remaining, bal') = reduceExcessFirst amount bal
  in  applyCreditSpread config remaining bal'
applyComputationalSpread Debit config amount bal =
  applyDebitSpread config amount bal

-----------------------------------------------------------------------
-- Credit internals
-----------------------------------------------------------------------

-- | If the current excess is positive, absorb the credit against it first.
reduceExcessFirst :: Money -> Balance -> (Money, Balance)
reduceExcessFirst amount bal
  | ex <= 0        = (amount, bal)
  | ex >= amount   = (0,      bal { balExcess = ex - amount })
  | otherwise      = (amount - ex, bal { balExcess = 0 })
  where ex = balExcess bal

-- | Add the remaining credit amount to the first component in the spread.
-- Subsequent components receive zero (matching Java's loop that zeros
-- @remainingAmount@ after the first iteration).
applyCreditSpread :: SpreadConfig -> Money -> Balance -> Balance
applyCreditSpread config remaining bal =
  fst $ foldl' step (bal, remaining) config
  where
    step (b, rem') comp = (setComponent comp (getComponent comp b + rem') b, 0)

-----------------------------------------------------------------------
-- Debit internals
-----------------------------------------------------------------------

-- | Reduce components in order.  Overflow goes to excess.
applyDebitSpread :: SpreadConfig -> Money -> Balance -> Balance
applyDebitSpread config amount bal =
  let (finalBal, leftover) = foldl' step (bal, amount) config
  in  if leftover > 0
      then finalBal { balExcess = balExcess finalBal + leftover }
      else finalBal
  where
    step (b, remaining) comp
      | remaining <= 0 = (b, 0)
      | current >= remaining =
          (setComponent comp (current - remaining) b, 0)
      | otherwise =
          (setComponent comp 0 b, remaining - current)
      where current = getComponent comp b
