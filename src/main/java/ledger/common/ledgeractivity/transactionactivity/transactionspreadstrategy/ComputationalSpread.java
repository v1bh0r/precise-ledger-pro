package ledger.common.ledgeractivity.transactionactivity.transactionspreadstrategy;

import ledger.common.TransactionSpreadStrategy;
import ledger.model.Balance;
import ledger.model.BalanceComponent;
import ledger.model.Direction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.javamoney.moneta.Money;
import org.jetbrains.annotations.NotNull;

import javax.money.MonetaryAmount;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * This class represents a computational spread strategy.
 * The amount is spread in the order of the spread configuration such as principal, interest, fee
 * In case the balance is reduced to zero, the remaining amount always goes to the excess component.
 */
@AllArgsConstructor
@Getter
@Setter
public class ComputationalSpread extends TransactionSpreadStrategy {
    @NonNull
    private MonetaryAmount amount;
    @NonNull
    private Direction direction;
    private LinkedHashSet<BalanceComponent> spreadConfiguration;

    ComputationalSpread(@NotNull MonetaryAmount amount, @NonNull Direction direction) {
        this.direction = direction;
        this.amount = amount;
        this.spreadConfiguration = new LinkedHashSet<>(List.of(BalanceComponent.FEES, BalanceComponent.INTEREST, BalanceComponent.PRINCIPAL));
    }

    @Override
    public Balance applyTo(Balance balance) {
        MonetaryAmount remainingAmount = amount;
        Balance updatedBalance = balance;

        if (Direction.CREDIT.equals(direction)) {
            for (BalanceComponent component : spreadConfiguration) {
                updatedBalance = updateBalanceForIncrease(updatedBalance, component, remainingAmount);
                remainingAmount = Money.of(0, amount.getCurrency());
            }
        } else if (Direction.DEBIT.equals(direction)) {
            for (BalanceComponent component : spreadConfiguration) {
                var result = updateBalanceForDecrease(updatedBalance, component, remainingAmount);
                updatedBalance = result.balance();
                remainingAmount = result.remainingAmount();

                if (remainingAmount.isZero() || remainingAmount.isNegative()) {
                    break;
                }
            }

            // Handle excess in case of decrease and remaining amount
            if (!remainingAmount.isZero()) {
                updatedBalance = new Balance(updatedBalance.principal(), updatedBalance.interest(), updatedBalance.fee(), updatedBalance.excess().add(remainingAmount));
            }
        }

        return updatedBalance;
    }

    private Balance updateBalanceForIncrease(Balance balance, BalanceComponent component, MonetaryAmount amount) {
        MonetaryAmount current = balance.get(component);
        return new Balance(component == BalanceComponent.PRINCIPAL ? current.add(amount) : balance.principal(), component == BalanceComponent.INTEREST ? current.add(amount) : balance.interest(), component == BalanceComponent.FEES ? current.add(amount) : balance.fee(), balance.excess() // Excess is not affected by increase directly
        );
    }

    private Pair<Balance, MonetaryAmount> updateBalanceForDecrease(Balance balance, BalanceComponent component, MonetaryAmount amount) {
        MonetaryAmount current = balance.get(component);
        if (current.isGreaterThan(amount) || current.equals(amount)) {
            // Component can cover the amount
            return new Pair<>(new Balance(component == BalanceComponent.PRINCIPAL ? current.subtract(amount) : balance.principal(), component == BalanceComponent.INTEREST ? current.subtract(amount) : balance.interest(), component == BalanceComponent.FEES ? current.subtract(amount) : balance.fee(), balance.excess()), // Excess is not directly affected
                    Money.of(0, amount.getCurrency()));
        } else {
            // Component cannot cover the whole amount; remainder goes to next component or excess
            MonetaryAmount remainder = amount.subtract(current);
            return new Pair<>(new Balance(component == BalanceComponent.PRINCIPAL ? Money.of(0, amount.getCurrency()) : balance.principal(), component == BalanceComponent.INTEREST ? Money.of(0, amount.getCurrency()) : balance.interest(), component == BalanceComponent.FEES ? Money.of(0, amount.getCurrency()) : balance.fee(), balance.excess()), // Excess is not directly affected
                    remainder);
        }
    }

    @AllArgsConstructor
    private static class Pair<T1, T2> {
        private T1 balance;
        private T2 remainingAmount;

        public T1 balance() {
            return balance;
        }

        public T2 remainingAmount() {
            return remainingAmount;
        }
    }
}
