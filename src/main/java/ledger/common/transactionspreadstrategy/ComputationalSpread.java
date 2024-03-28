package ledger.common.transactionspreadstrategy;

import ledger.common.TransactionSpreadStrategy;
import ledger.model.Balance;
import ledger.model.BalanceComponent;
import ledger.model.Direction;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import javax.money.MonetaryAmount;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * This class represents a computational spread strategy.
 * The amount is spread in the order of the spread configuration such as principal, interest, fees
 * In case the balance is reduced to zero, the remaining amount always goes to the excess component.
 */
@AllArgsConstructor
public class ComputationalSpread extends TransactionSpreadStrategy {
    @NonNull
    private MonetaryAmount amount;
    @NonNull
    private Direction direction;
    private LinkedHashSet<BalanceComponent> spreadConfiguration;

    ComputationalSpread(@NotNull MonetaryAmount amount, @NonNull Direction direction) {
        this.amount = amount;
        this.spreadConfiguration = new LinkedHashSet<>(List.of(BalanceComponent.FEES, BalanceComponent.INTEREST, BalanceComponent.PRINCIPAL));
    }

    @Override
    public Balance applyTo(Balance balance) {
        // In case of decrease direction, the amount is spread based on the config until the component balance is zero.
        // Eg. P I F E - 100 50 25 0. If the amount is 75 and spread as F I P, the balance will be reduced to 100 0 0 0
        // In case of increase direction, the amount simply goes to the first component in the spread configuration
        // We can get the MonetaryAmount of the component from the Balance by a get operation. Balance is a record and is immutable.
        return null;
    }
}
