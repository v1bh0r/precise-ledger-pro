-- | Daily interest calculation — pure translation of
-- @DailyInterestCalculator.java@ and @DailyInterestCalculationCommand.java@.
module Ledger.Interest
  ( calculateDailyInterest
  , getApplicableRate
  ) where

import Ledger.Types

-- | @calculateDailyInterest principal rate daysInYear@
--
-- > principal * rate / daysInYear
--
-- This is the core daily accrual formula.
calculateDailyInterest :: Money -> Double -> Int -> Money
calculateDailyInterest principal rate daysInYear =
  principal * rate / fromIntegral daysInYear

-- | Find the interest rate in effect at a given time.
-- Returns 0 if no rate is found.
--
-- Java: filters rates where @effectiveAt < effectiveAt@, takes first.
-- Note: rates are assumed to be pre-sorted in descending order of effectiveAt.
getApplicableRate :: [InterestRate] -> LocalTime -> Double
getApplicableRate rates t =
  case filter (\r -> irEffectiveAt r < t) rates of
    (r:_) -> irRate r
    []    -> 0.0
