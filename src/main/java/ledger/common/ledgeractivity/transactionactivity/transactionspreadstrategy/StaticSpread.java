package ledger.common.ledgeractivity.transactionactivity.transactionspreadstrategy;

import ledger.common.TransactionSpreadStrategy;
import ledger.model.Balance;
import ledger.model.Direction;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StaticSpread extends TransactionSpreadStrategy {
    Balance amountAllocation;
    Direction direction;

    @Override
    public Balance applyTo(Balance balance) {
        Balance newBalance;
        if (direction == Direction.CREDIT) {
            newBalance = balance.add(amountAllocation);
        } else {
            newBalance = balance.subtract(amountAllocation);
        }
        return newBalance;
    }
}
