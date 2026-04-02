-- | LedgerService — orchestration algorithms for applying activities,
-- handling backdated entries, reversals, and retroactive reconciliation.
--
-- This is the pure Haskell translation of @LedgerService.java@.
--
-- The key insight: in Haskell every operation returns a new @(Ledger, LedgerClock)@
-- pair.  There is no mutation, no side effects.  The recursive structure of
-- backdated processing and reversal replay is expressed as pure function
-- composition.
--
-- **External dependencies** (database lookups in the Java version) are
-- modelled as function parameters, making the algorithms fully testable
-- without any I/O.
module Ledger.Service
  ( -- * Core orchestration
    applyLedgerActivities
  , applyLedgerActivity
  , syncWithRetroactiveLedger
  , reverseLedgerActivity

    -- * Activity lookup (dependency injection)
  , ActivityLookup (..)
  ) where

import Data.List (foldl', sortBy)
import Data.Ord  (comparing)
import qualified Data.Set as Set

import Ledger.Types
import Ledger.Balance   (addBalance, negateBalance, subtractBalance,
                         totalAmount)
import Ledger.Core      (addEntry, calculateTotalImpact, currentBalance,
                         entriesSortedBy, isBackdated,
                         rollbackToEntryBefore, rollbackToEntryBeforeActivity)
import Ledger.Activity  (TemporalContext, generateEntries)

-- | Abstraction over the database lookups that the Java @LedgerService@
-- performs via @LoanService@.  In a pure setting we pass these as a record
-- of functions (the "ports" in hexagonal architecture).
data ActivityLookup = ActivityLookup
  { -- | Equivalent of @getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBefore@.
    --   Given (loanId, effectiveAt, createdBefore), return matching activities
    --   sorted by effectiveAt.
    lookupActivitiesAfter :: String -> LocalTime -> LocalTime
                          -> [LedgerActivity]
    -- | Equivalent of @getLedgerActivitiesCreatedSinceButBeforeCreatedAt@.
    --   Given (loanId, activityType, activityId, createdBefore), return
    --   activities created after the identified activity, before createdBefore.
  , lookupActivitiesCreatedSince :: String -> ActivityType -> ActivityId
                                 -> LocalTime -> [LedgerActivity]
  }

-----------------------------------------------------------------------
-- applyLedgerActivities
-----------------------------------------------------------------------

-- | Apply a list of ledger activities in order, producing a new ledger
-- and an advanced clock.
--
-- Translates @LedgerService.applyLedgerActivities()@.
-- The recursion for backdated entries is handled internally.
applyLedgerActivities
  :: ActivityLookup
  -> Maybe TemporalContext
  -> [LedgerActivity]
  -> Ledger
  -> LedgerClock
  -> (Ledger, LedgerClock)
applyLedgerActivities lkup ctx activities ledger clock =
  foldl' step (ledger, clock) activities
  where
    step (l, c) act = applyLedgerActivity lkup ctx act l c

-----------------------------------------------------------------------
-- applyLedgerActivity
-----------------------------------------------------------------------

-- | Apply a single activity.  If the activity is backdated relative to
-- the ledger, it triggers the retroactive processing path.
--
-- Translates @LedgerService.applyLedgerActivity()@.
applyLedgerActivity
  :: ActivityLookup
  -> Maybe TemporalContext
  -> LedgerActivity
  -> Ledger
  -> LedgerClock
  -> (Ledger, LedgerClock)
applyLedgerActivity lkup ctx act ledger clock
  | isBackdated act ledger = performBackdatedActivity lkup ctx act ledger clock
  | otherwise =
      let ledger' = generateEntries act ctx ledger
          clock'  = advanceClock (laCreatedAt act) clock
      in  (ledger', clock')

-----------------------------------------------------------------------
-- performBackdatedActivity
-----------------------------------------------------------------------

-- | Handle a backdated activity by:
--   1. Rolling back the ledger to before the backdated effective date (fork)
--   2. Applying the activity to the fork
--   3. Adding the new fork entries to the primary ledger
--   4. Re-applying subsequent activities to the fork
--   5. Syncing the fork back into the primary ledger
--
-- Translates @LedgerService.performBackdatedActivity()@.
performBackdatedActivity
  :: ActivityLookup
  -> Maybe TemporalContext
  -> LedgerActivity
  -> Ledger
  -> LedgerClock
  -> (Ledger, LedgerClock)
performBackdatedActivity lkup ctx act ledger clock =
  let -- Step 1: Fork the ledger at the backdated point
      retroLedger = rollbackToEntryBefore (laEffectiveAt act) ledger
      forkIdx     = length (ledgerEntries retroLedger)

      -- Step 2: Apply the backdated activity to the fork
      retroLedger' = generateEntries act ctx retroLedger
      clock'       = advanceClock (laCreatedAt act) clock

      -- Step 3: Copy new fork entries into primary ledger
      newEntries = drop forkIdx (ledgerEntries retroLedger')
      ledger'    = foldl' (flip addEntry) ledger newEntries

      -- Step 4: Re-apply subsequent activities to the fork
      subsequent = sortBy (comparing laEffectiveAt)
                     (lookupActivitiesAfter lkup
                       (ledgerLoanId ledger)
                       (laEffectiveAt act)
                       (clockNow clock'))
      (retroLedger'', clock'') =
        applyLedgerActivities lkup ctx subsequent retroLedger' clock'

      -- Step 5: Sync the fork back
      ledger'' = syncWithRetroactiveLedger
                   ledger'
                   retroLedger''
                   (clockNow clock'')
                   (laCreatedAt act)
                   forkIdx

  in (ledger'', clock'')

-----------------------------------------------------------------------
-- syncWithRetroactiveLedger
-----------------------------------------------------------------------

-- | Reconcile a primary ledger with a retroactive (forked) ledger by
-- comparing the total impact of each activity and creating adjustment
-- entries for any differences.
--
-- Translates @LedgerService.syncWithRetroactiveLedger()@.
--
-- Pure: produces a new primary ledger.
syncWithRetroactiveLedger
  :: Ledger       -- ^ primary ledger
  -> Ledger       -- ^ retroactive ledger
  -> LocalTime    -- ^ currentTime (for adjustment createdAt)
  -> LocalTime    -- ^ adjustmentEffectiveAt
  -> Int          -- ^ forkIndex
  -> Ledger       -- ^ updated primary ledger
syncWithRetroactiveLedger primary retro currentTime adjEffAt forkIdx
  | null retroEntries || null primaryEntries = primary
  | otherwise = processEntries primary Set.empty forkIdx
  where
    retroEntries   = entriesSortedBy leEffectiveAt retro
    primaryEntries = entriesSortedBy leEffectiveAt primary

    processEntries :: Ledger -> Set.Set String -> Int -> Ledger
    processEntries prim processed idx
      | idx >= length retroEntries = prim
      | otherwise =
          let retroEntry  = retroEntries !! idx
              aType       = leSourceLedgerActivityType retroEntry
              aId         = leSourceLedgerActivityId   retroEntry
              aKey        = activityKey aType aId
          in if Set.member aKey processed
             then processEntries prim processed (idx + 1)
             else
              let primImpact  = calculateTotalImpact aType aId prim
                  retroImpact = calculateTotalImpact aType aId retro
              in case primImpact of
               Nothing ->
                 -- Activity not in primary: copy entry directly
                 let prim' = addEntry retroEntry prim
                 in  processEntries prim' (Set.insert aKey processed) (idx + 1)
               Just pImpact ->
                 let ri = case retroImpact of
                            Just x  -> x
                            Nothing -> zeroBalance
                 in  if ri == pImpact
                     -- Impacts match: no adjustment needed
                     then processEntries prim (Set.insert aKey processed) (idx + 1)
                     else
                       -- Create an adjustment entry for the difference
                       let diff    = subtractBalance ri pImpact
                           curBal  = currentBalance prim
                           adjEntry = LedgerEntry
                             { leLoanId                   = ledgerLoanId prim
                             , leEntryId                  = ""
                             , leEntryType                = "Adjustment"
                             , leAmount                   = totalAmount diff
                             , lePrincipal                = balPrincipal diff
                             , leInterest                 = balInterest  diff
                             , leFee                      = balFee       diff
                             , leExcess                   = balExcess    diff
                             , lePrincipalBalance         = balPrincipal curBal
                                                          + balPrincipal diff
                             , leInterestBalance          = balInterest curBal
                                                          + balInterest diff
                             , leFeeBalance               = balFee curBal
                                                          + balFee diff
                             , leExcessBalance            = balExcess curBal
                                                          + balExcess diff
                             , leEffectiveAt              = adjEffAt
                             , leCreatedAt                = currentTime
                             , leSourceLedgerActivityType = aType
                             , leSourceLedgerActivityId   = aId
                             }
                           prim' = addEntry adjEntry prim
                       in  processEntries prim'
                             (Set.insert aKey processed) (idx + 1)

-----------------------------------------------------------------------
-- reverseLedgerActivity
-----------------------------------------------------------------------

-- | Reverse a previously applied activity by:
--   1. Creating a compensation entry (negated impact)
--   2. Rolling back to before the reversed activity
--   3. Re-applying subsequent activities to the fork
--   4. Syncing the fork back
--
-- Translates @LedgerService.reverseLedgerActivity()@.
reverseLedgerActivity
  :: ActivityLookup
  -> Maybe TemporalContext
  -> LedgerActivity          -- ^ must be ReversalActivityData
  -> Ledger
  -> LedgerClock
  -> Either String (Ledger, LedgerClock)
reverseLedgerActivity lkup ctx act ledger clock = case act of
  ReversalActivityData _ _ _ _ effAt creAt rType rId ->
    let revType = rType
        revId   = rId
    in case calculateTotalImpact revType revId ledger of
         Nothing ->
           Left $ "Cannot reverse: no entries found for activity "
               ++ revType ++ "/" ++ revId
         Just impact ->
           let -- Step 1: Compensation entry
               negated = negateBalance impact
               curBal  = currentBalance ledger
               newBal  = addBalance curBal negated
               compEntry = LedgerEntry
                 { leLoanId                   = ledgerLoanId ledger
                 , leEntryId                  = ""
                 , leEntryType                = "Reversal"
                 , leAmount                   = totalAmount negated
                 , lePrincipal                = balPrincipal negated
                 , leInterest                 = balInterest  negated
                 , leFee                      = balFee       negated
                 , leExcess                   = balExcess    negated
                 , lePrincipalBalance         = balPrincipal newBal
                 , leInterestBalance          = balInterest  newBal
                 , leFeeBalance               = balFee       newBal
                 , leExcessBalance            = balExcess    newBal
                 , leEffectiveAt              = effAt
                 , leCreatedAt                = creAt
                 , leSourceLedgerActivityType = revType
                 , leSourceLedgerActivityId   = revId
                 }
               ledger1 = addEntry compEntry ledger

               -- Step 2: Fork before the reversed activity
               retroLedger = rollbackToEntryBeforeActivity revType revId ledger1
               forkIdx     = length (ledgerEntries retroLedger)

               -- Step 3: Re-apply subsequent activities (excluding this reversal)
               subsequent =
                 sortBy (comparing Ledger.Types.laEffectiveAt)
                   $ filter (\a -> a /= act)
                   $ lookupActivitiesCreatedSince lkup
                       (ledgerLoanId ledger) revType revId (clockNow clock)
               (retroLedger', clock') =
                 applyLedgerActivities lkup ctx subsequent retroLedger clock

               -- Step 4: Sync back
               ledger2 = syncWithRetroactiveLedger
                           ledger1 retroLedger' (clockNow clock')
                           creAt forkIdx

           in Right (ledger2, clock')

  _ -> Left "reverseLedgerActivity requires a ReversalActivityData"
