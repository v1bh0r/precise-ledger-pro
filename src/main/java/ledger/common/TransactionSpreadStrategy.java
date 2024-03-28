package ledger.common;

import ledger.model.Balance;

public abstract class TransactionSpreadStrategy {
    public abstract Balance applyTo(Balance balance);
}
