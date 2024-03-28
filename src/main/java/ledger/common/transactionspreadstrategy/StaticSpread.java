package ledger.common.transactionspreadstrategy;

import ledger.common.TransactionSpreadStrategy;
import ledger.model.Balance;
import ledger.model.Direction;

public class StaticSpread extends TransactionSpreadStrategy {
    Balance amountAllocation;

    Direction direction;

    @Override
    public Balance applyTo(Balance balance) {
        Balance newBalance;
        if (direction == Direction.INCREASE) {
            newBalance = balance.add(amountAllocation);
        } else {
            newBalance = balance.subtract(amountAllocation);
        }
        return newBalance;
    }
}
