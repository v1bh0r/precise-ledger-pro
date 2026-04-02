-- | Balance arithmetic — pure functions over the four-component balance.
module Ledger.Balance
  ( addBalance
  , subtractBalance
  , negateBalance
  , totalAmount
  , getComponent
  , setComponent
  ) where

import Ledger.Types

-- | Component-wise addition of two balances.
--
-- > addBalance (Balance 100 10 5 0) (Balance 200 20 10 0)
-- >   == Balance 300 30 15 0
addBalance :: Balance -> Balance -> Balance
addBalance a b = Balance
  { balPrincipal = balPrincipal a + balPrincipal b
  , balInterest  = balInterest  a + balInterest  b
  , balFee       = balFee       a + balFee       b
  , balExcess    = balExcess    a + balExcess    b
  }

-- | Component-wise subtraction.
subtractBalance :: Balance -> Balance -> Balance
subtractBalance a b = Balance
  { balPrincipal = balPrincipal a - balPrincipal b
  , balInterest  = balInterest  a - balInterest  b
  , balFee       = balFee       a - balFee       b
  , balExcess    = balExcess    a - balExcess    b
  }

-- | Negate every component.
negateBalance :: Balance -> Balance
negateBalance b = Balance
  { balPrincipal = negate (balPrincipal b)
  , balInterest  = negate (balInterest  b)
  , balFee       = negate (balFee       b)
  , balExcess    = negate (balExcess    b)
  }

-- | Sum of all four components.
totalAmount :: Balance -> Money
totalAmount b = balPrincipal b + balInterest b + balFee b + balExcess b

-- | Project a single component from a balance.
getComponent :: BalanceComponent -> Balance -> Money
getComponent Principal b = balPrincipal b
getComponent Interest  b = balInterest  b
getComponent Fees      b = balFee       b
getComponent Excess    b = balExcess    b

-- | Return a new balance with one component replaced.
setComponent :: BalanceComponent -> Money -> Balance -> Balance
setComponent Principal v b = b { balPrincipal = v }
setComponent Interest  v b = b { balInterest  = v }
setComponent Fees      v b = b { balFee       = v }
setComponent Excess    v b = b { balExcess    = v }
