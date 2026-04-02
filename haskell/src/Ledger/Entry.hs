-- | LedgerEntry helper operations.
module Ledger.Entry
  ( idempotencyKey
  , entryBalance
  , entryBalanceChange
  , updateEntryBalances
  ) where

import Ledger.Types

-- | Compute the idempotency key for duplicate detection.
--
-- Key is based on semantic identity: (loanId, entryType, activityType,
-- activityId, effectiveAt).  @leEntryId@ is excluded because it is always
-- @""@ in the computation layer — it is assigned by the persistence layer
-- and contributes nothing to uniqueness here (B3 fix).
idempotencyKey :: LedgerEntry -> IdempotencyKey
idempotencyKey e = concat
  [ leLoanId e, "-"
  , leEntryType e, "-"
  , leSourceLedgerActivityType e, "-"
  , leSourceLedgerActivityId e, "-"
  , show (leEffectiveAt e)
  ]

-- | Extract the running balance snapshot from an entry.
entryBalance :: LedgerEntry -> Balance
entryBalance e = Balance
  { balPrincipal = lePrincipalBalance e
  , balInterest  = leInterestBalance  e
  , balFee       = leFeeBalance       e
  , balExcess    = leExcessBalance    e
  }

-- | Extract the change (delta) that this entry represents.
entryBalanceChange :: LedgerEntry -> Balance
entryBalanceChange e = Balance
  { balPrincipal = lePrincipal e
  , balInterest  = leInterest  e
  , balFee       = leFee       e
  , balExcess    = leExcess    e
  }

-- | Recompute the running balance fields of an entry given the current ledger
-- balance before this entry.  Applies @roundMoney@ to every accumulated total,
-- matching Java's @Monetary.getDefaultRounding()@ on @MonetaryAmount@ sums
-- (B2 fix: previously un-rounded Double accumulated indefinitely).
updateEntryBalances :: Balance -> LedgerEntry -> LedgerEntry
updateEntryBalances cur e = e
  { lePrincipalBalance = roundMoney (balPrincipal cur + lePrincipal e)
  , leInterestBalance  = roundMoney (balInterest  cur + leInterest  e)
  , leFeeBalance       = roundMoney (balFee       cur + leFee       e)
  , leExcessBalance    = roundMoney (balExcess    cur + leExcess    e)
  }
