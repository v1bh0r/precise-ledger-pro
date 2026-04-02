-- | Activity processing — generates ledger entries from activities.
--
-- Translates the @generateLedgerEntries()@ methods from Java's
-- @Transaction@, @StartOfDay@, and @ReversalActivity@ classes.
--
-- Each function is pure: it takes a ledger (and context) and returns
-- a new ledger with the entries appended.
module Ledger.Activity
  ( generateEntries
  , TemporalContext (..)
  ) where

import Ledger.Types
import Ledger.Balance   (subtractBalance)
import Ledger.Core      (addEntry, currentBalance)
import Ledger.Spread    (applyComputationalSpread, defaultSpreadConfig,
                         parseSpreadConfig)
import Ledger.Interest  (calculateDailyInterest, getApplicableRate)

-- | Context passed to temporal activities (StartOfDay) for interest
-- calculation.  Replaces the Java @TemporalActivityContext@ map.
data TemporalContext = TemporalContext
  { tcInterestRates :: ![InterestRate]
  , tcDaysInYear    :: !Int
  , tcCurrencyCode  :: !String
  } deriving (Show, Eq)

-- | Generate ledger entries for an activity and append them to the ledger.
--
-- This does NOT handle the backdated/reversal orchestration — that lives
-- in @Ledger.Service@.  This function only handles the "happy path" entry
-- generation, equivalent to @generateLedgerEntries()@ in each Java subclass.
generateEntries :: LedgerActivity -> Maybe TemporalContext -> Ledger -> Ledger
generateEntries activity ctx ledger = case activity of
  TransactionActivity{} -> generateTransactionEntries activity ledger
  StartOfDayActivity{}  -> generateStartOfDayEntries activity ctx ledger
  ReversalActivityData{} -> ledger  -- reversals are handled by LedgerService

-----------------------------------------------------------------------
-- Transaction
-----------------------------------------------------------------------

-- | Translates @Transaction.generateLedgerEntries()@.
--
-- 1. Get current balance
-- 2. Apply spread strategy to compute new balance
-- 3. Compute the delta (change)
-- 4. Create and add a single entry
generateTransactionEntries :: LedgerActivity -> Ledger -> Ledger
generateTransactionEntries act ledger =
  let bal     = currentBalance ledger
      config  = if null (laSpread act)
                then defaultSpreadConfig
                else parseSpreadConfig (laSpread act)
      newBal  = applyComputationalSpread (laDirection act) config (laAmount act) bal
      change  = subtractBalance newBal bal
      entry   = LedgerEntry
        { leLoanId                   = ledgerLoanId ledger
        , leEntryId                  = ""  -- assigned by persistence layer
        , leEntryType                = laCommonName act
        , leAmount                   = laAmount act
        , lePrincipal                = balPrincipal change
        , leInterest                 = balInterest  change
        , leFee                      = balFee       change
        , leExcess                   = balExcess    change
        , lePrincipalBalance         = balPrincipal newBal
        , leInterestBalance          = balInterest  newBal
        , leFeeBalance               = balFee       newBal
        , leExcessBalance            = balExcess    newBal
        , leEffectiveAt              = laEffectiveAt act
        , leCreatedAt                = laCreatedAt act
        , leSourceLedgerActivityType = laActivityType act
        , leSourceLedgerActivityId   = laActivityId act
        }
  in addEntry entry ledger

-----------------------------------------------------------------------
-- StartOfDay (daily interest accrual)
-----------------------------------------------------------------------

-- | Translates @StartOfDay.generateLedgerEntries()@ + @DailyInterestCalculationCommand@.
--
-- Calculates daily interest on the principal balance and creates an
-- interest accrual entry.  Entry is skipped if interest is zero.
generateStartOfDayEntries :: LedgerActivity -> Maybe TemporalContext -> Ledger -> Ledger
generateStartOfDayEntries _act Nothing ledger = ledger  -- no context → no interest
generateStartOfDayEntries act (Just ctx) ledger =
  let bal       = currentBalance ledger
      rate      = getApplicableRate (tcInterestRates ctx) (laEffectiveAt act)
      interest  = calculateDailyInterest (balPrincipal bal) rate (tcDaysInYear ctx)
  in if interest == 0
     then ledger
     else let entry = LedgerEntry
                { leLoanId                   = ledgerLoanId ledger
                , leEntryId                  = ""
                , leEntryType                = "Interest Accrual"
                , leAmount                   = interest
                , lePrincipal                = 0
                , leInterest                 = interest
                , leFee                      = 0
                , leExcess                   = 0
                , lePrincipalBalance         = balPrincipal bal
                , leInterestBalance          = balInterest bal + interest
                , leFeeBalance               = balFee bal
                , leExcessBalance            = balExcess bal
                , leEffectiveAt              = laEffectiveAt act
                , leCreatedAt                = laCreatedAt act
                , leSourceLedgerActivityType = laActivityType act
                , leSourceLedgerActivityId   = laActivityId act
                }
          in addEntry entry ledger
