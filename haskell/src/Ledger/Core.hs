-- | Core Ledger operations — pure, immutable transformations.
--
-- Every function returns a new Ledger; nothing is mutated.
-- This is the Haskell equivalent of @Ledger.java@.
module Ledger.Core
  ( addEntry
  , currentBalance
  , balanceAt
  , entriesSortedBy
  , rollbackToEntryBefore
  , rollbackToEntryBeforeActivity
  , calculateTotalImpact
  , isBackdated
  ) where

import Data.List (sortBy, foldl')
import Data.Ord  (comparing)

import Ledger.Types
import Ledger.Balance (addBalance)
import Ledger.Entry   (idempotencyKey, entryBalance, entryBalanceChange,
                        updateEntryBalances)

-- | Add an entry to the ledger, recomputing running balances and enforcing
-- idempotency.  Returns the ledger unchanged if the entry's idempotency key
-- already exists.
--
-- This is the pure equivalent of @Ledger.addEntry()@ in Java.
addEntry :: LedgerEntry -> Ledger -> Ledger
addEntry entry ledger
  | isDuplicate = ledger
  | otherwise   = ledger { ledgerEntries = ledgerEntries ledger ++ [updated] }
  where
    updated     = updateEntryBalances (currentBalance ledger) entry
    newKey      = idempotencyKey updated
    isDuplicate = any (\e -> idempotencyKey e == newKey) (ledgerEntries ledger)

-- | The ledger's current balance — the balance after the last entry,
-- or the start balance if the ledger is empty.
currentBalance :: Ledger -> Balance
currentBalance ledger = case ledgerEntries ledger of
  [] -> ledgerStartBalance ledger
  es -> entryBalance (last es)

-- | Balance at a given point in time (by effectiveAt).
balanceAt :: LocalTime -> Ledger -> Balance
balanceAt t ledger =
  case filter (\e -> leEffectiveAt e <= t) sorted of
    [] -> ledgerStartBalance ledger
    es -> entryBalance (last es)
  where
    sorted = entriesSortedBy leEffectiveAt ledger

-- | Entries sorted by an arbitrary projection.
entriesSortedBy :: Ord b => (LedgerEntry -> b) -> Ledger -> [LedgerEntry]
entriesSortedBy proj ledger =
  sortBy (comparing proj) (ledgerEntries ledger)

-- | Fork the ledger keeping only entries effective on or before a datetime.
-- This is the time-based rollback used for backdated entries.
rollbackToEntryBefore :: LocalTime -> Ledger -> Ledger
rollbackToEntryBefore t ledger = ledger
  { ledgerEntries = filter (\e -> leEffectiveAt e <= t) (ledgerEntries ledger) }

-- | Fork the ledger keeping only entries before a specific activity.
-- Used by the reversal algorithm.
rollbackToEntryBeforeActivity :: ActivityType -> ActivityId -> Ledger -> Ledger
rollbackToEntryBeforeActivity aType aId ledger =
  case findIndex' matchesActivity (ledgerEntries ledger) of
    Nothing -> ledger  -- activity not found; return full ledger
    Just i  -> ledger { ledgerEntries = take i (ledgerEntries ledger) }
  where
    matchesActivity e = leSourceLedgerActivityType e == aType
                     && leSourceLedgerActivityId   e == aId

-- | Calculate the total impact (sum of balance changes) of all entries
-- originating from a given activity.  Returns @Nothing@ if no entries match.
--
-- This is the pure equivalent of @Ledger.calculateTotalImpact()@.
calculateTotalImpact :: ActivityType -> ActivityId -> Ledger -> Maybe Balance
calculateTotalImpact aType aId ledger =
  case matching of
    [] -> Nothing
    _  -> Just (foldl' addBalance zeroBalance (map entryBalanceChange matching))
  where
    matching = filter (\e -> leSourceLedgerActivityType e == aType
                          && leSourceLedgerActivityId   e == aId)
                      (ledgerEntries ledger)

-- | Is an activity backdated relative to the ledger's last effective date?
isBackdated :: LedgerActivity -> Ledger -> Bool
isBackdated activity ledger =
  case entriesSortedBy leEffectiveAt ledger of
    [] -> False
    es -> laEffectiveAt activity < leEffectiveAt (last es)

-- Internal: find the index of the first element satisfying a predicate.
findIndex' :: (a -> Bool) -> [a] -> Maybe Int
findIndex' p = go 0
  where
    go _ []     = Nothing
    go i (x:xs)
      | p x      = Just i
      | otherwise = go (i + 1) xs
