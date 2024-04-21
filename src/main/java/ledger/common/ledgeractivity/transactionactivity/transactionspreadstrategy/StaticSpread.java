package ledger.common.ledgeractivity.transactionactivity.transactionspreadstrategy;

import ledger.common.TransactionSpreadStrategy;
import ledger.model.Balance;
import ledger.model.Direction;

public class StaticSpread extends TransactionSpreadStrategy {
    Balance amountAllocation;

    public StaticSpread(Balance amountAllocation, Direction direction) {
        super(direction);
        this.amountAllocation = amountAllocation;
    }

    @Override
    public Balance applyTo(Balance balance) {
        Balance newBalance;
        if (this.getDirection() == Direction.CREDIT) {
            newBalance = balance.add(amountAllocation);
        } else {
            newBalance = balance.subtract(amountAllocation);
        }
        return newBalance;
    }
}
