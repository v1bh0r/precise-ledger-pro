-- | LedgerEntry helper operations.
module Ledger.Entry
  ( idempotencyKey
  , entryBalance
  , entryBalanceChange
  , updateEntryBalances
  ) where

import Ledger.Types

-- | Compute the idempotency key for duplicate detection.
idempotencyKey :: LedgerEntry -> IdempotencyKey
idempotencyKey e = concat
  [ leLoanId e, "-"
  , leEntryId e, "-"
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

-- | Recompute the running balance fields of an entry given
-- the current ledger balance before this entry.
updateEntryBalances :: Balance -> LedgerEntry -> LedgerEntry
updateEntryBalances cur e = e
  { lePrincipalBalance = balPrincipal cur + lePrincipal e
  , leInterestBalance  = balInterest  cur + leInterest  e
  , leFeeBalance       = balFee       cur + leFee       e
  , leExcessBalance    = balExcess    cur + leExcess    e
  }
