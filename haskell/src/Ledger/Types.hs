-- | Core domain types for the Precise Ledger system.
--
-- All types are immutable. Ledger operations produce new values rather
-- than mutating existing ones.
module Ledger.Types
  ( -- * Monetary
    Money
  , Direction (..)
  , BalanceComponent (..)
    -- * Balance
  , Balance (..)
  , zeroBalance
    -- * Ledger Entry
  , LedgerEntry (..)
  , IdempotencyKey
    -- * Ledger
  , Ledger (..)
    -- * Activity
  , ActivityId
  , ActivityType
  , LedgerActivity (..)
  , activityKey
    -- * Clock
  , LedgerClock (..)
  , advanceClock
  , defaultClock
    -- * Interest
  , InterestRate (..)
    -- * Time re-export
  , LocalTime (..)
  , minLocalDateTime
  ) where

import Data.Time (LocalTime(..), fromGregorian, midnight)

type Money = Double

data Direction = Credit | Debit
  deriving (Eq, Show)

data BalanceComponent = Principal | Interest | Fees | Excess
  deriving (Eq, Ord, Show, Enum, Bounded)

data Balance = Balance
  { balPrincipal :: !Money
  , balInterest  :: !Money
  , balFee       :: !Money
  , balExcess    :: !Money
  } deriving (Show)

instance Eq Balance where
  a == b = balPrincipal a == balPrincipal b
        && balInterest  a == balInterest  b
        && balFee       a == balFee       b
        && balExcess    a == balExcess    b

zeroBalance :: Balance
zeroBalance = Balance 0 0 0 0

type IdempotencyKey = String

data LedgerEntry = LedgerEntry
  { leLoanId                   :: !String
  , leEntryId                  :: !String
  , leEntryType                :: !String
  , leAmount                   :: !Money
  , lePrincipal                :: !Money
  , leInterest                 :: !Money
  , leFee                      :: !Money
  , leExcess                   :: !Money
  , lePrincipalBalance         :: !Money
  , leInterestBalance          :: !Money
  , leFeeBalance               :: !Money
  , leExcessBalance            :: !Money
  , leEffectiveAt              :: !LocalTime
  , leCreatedAt                :: !LocalTime
  , leSourceLedgerActivityType :: !String
  , leSourceLedgerActivityId   :: !String
  } deriving (Show, Eq)

data Ledger = Ledger
  { ledgerLoanId       :: !String
  , ledgerStartBalance :: !Balance
  , ledgerEntries      :: ![LedgerEntry]
  , ledgerCurrency     :: !String
  } deriving (Show, Eq)

type ActivityId   = String
type ActivityType = String

data LedgerActivity
  = TransactionActivity
      { laLoanId       :: !String
      , laCommonName   :: !String
      , laActivityType :: !ActivityType
      , laActivityId   :: !ActivityId
      , laEffectiveAt  :: !LocalTime
      , laCreatedAt    :: !LocalTime
      , laAmount       :: !Money
      , laDirection    :: !Direction
      , laSpread       :: !String
      }
  | StartOfDayActivity
      { laLoanId       :: !String
      , laCommonName   :: !String
      , laActivityType :: !ActivityType
      , laActivityId   :: !ActivityId
      , laEffectiveAt  :: !LocalTime
      , laCreatedAt    :: !LocalTime
      }
  | ReversalActivityData
      { laLoanId                :: !String
      , laCommonName            :: !String
      , laActivityType          :: !ActivityType
      , laActivityId            :: !ActivityId
      , laEffectiveAt           :: !LocalTime
      , laCreatedAt             :: !LocalTime
      , laReversedActivityType  :: !ActivityType
      , laReversedActivityId    :: !ActivityId
      }
  deriving (Show, Eq)

activityKey :: ActivityType -> ActivityId -> String
activityKey aType aId = aType ++ "-" ++ aId

data LedgerClock = LedgerClock
  { clockNow :: !LocalTime
  } deriving (Show, Eq)

defaultClock :: LedgerClock
defaultClock = LedgerClock minLocalDateTime

advanceClock :: LocalTime -> LedgerClock -> LedgerClock
advanceClock t (LedgerClock now)
  | t > now   = LedgerClock t
  | otherwise = LedgerClock now

minLocalDateTime :: LocalTime
minLocalDateTime = LocalTime (fromGregorian 1970 1 1) midnight

data InterestRate = InterestRate
  { irLoanId      :: !String
  , irRate        :: !Double
  , irEffectiveAt :: !LocalTime
  } deriving (Show, Eq)
