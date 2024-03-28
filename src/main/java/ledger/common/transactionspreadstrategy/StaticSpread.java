package ledger.common.transactionspreadstrategy;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.TransactionSpreadStrategy;
import ledger.model.Balance;
import ledger.model.Direction;
import ledger.service.BalanceService;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class StaticSpread extends TransactionSpreadStrategy {
    private final BalanceService balanceService;
    Balance amountAllocation;
    Direction direction;

    @Override
    public Balance applyTo(Balance balance) {
        Balance newBalance;
        if (direction == Direction.INCREASE) {
            newBalance = balanceService.add(balance, amountAllocation);
        } else {
            newBalance = balanceService.subtract(balance, amountAllocation);
        }
        return newBalance;
    }
}
