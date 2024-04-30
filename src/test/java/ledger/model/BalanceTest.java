package ledger.model;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.money.MonetaryAmount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceTest {

    @Test
    @DisplayName("Should add two balances correctly")
    void shouldAddTwoBalancesCorrectly() {
        MonetaryAmount amount1 = Money.of(100.0, "USD");
        MonetaryAmount amount2 = Money.of(200.0, "USD");
        Balance balance1 = new Balance(amount1, amount1, amount1, amount1);
        Balance balance2 = new Balance(amount2, amount2, amount2, amount2);

        Balance result = balance1.add(balance2);

        assertEquals(Money.of(300.0, "USD"), result.principal());
        assertEquals(Money.of(300.0, "USD"), result.interest());
        assertEquals(Money.of(300.0, "USD"), result.fee());
        assertEquals(Money.of(300.0, "USD"), result.excess());
    }

    @Test
    @DisplayName("Should subtract two balances correctly")
    void shouldSubtractTwoBalancesCorrectly() {
        MonetaryAmount amount1 = Money.of(200.0, "USD");
        MonetaryAmount amount2 = Money.of(100.0, "USD");
        Balance balance1 = new Balance(amount1, amount1, amount1, amount1);
        Balance balance2 = new Balance(amount2, amount2, amount2, amount2);

        Balance result = balance1.subtract(balance2);

        assertEquals(Money.of(100.0, "USD"), result.principal());
        assertEquals(Money.of(100.0, "USD"), result.interest());
        assertEquals(Money.of(100.0, "USD"), result.fee());
        assertEquals(Money.of(100.0, "USD"), result.excess());
    }

    @Test
    @DisplayName("Should return true when two balances are equal")
    void shouldReturnTrueWhenTwoBalancesAreEqual() {
        MonetaryAmount amount = Money.of(100.0, "USD");
        Balance balance1 = new Balance(amount, amount, amount, amount);
        Balance balance2 = new Balance(amount, amount, amount, amount);

        boolean result = balance1.equals(balance2);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should negate a balance correctly")
    void shouldNegateABalanceCorrectly() {
        MonetaryAmount amount = Money.of(100.0, "USD");
        Balance balance = new Balance(amount, amount, amount, amount);

        Balance result = balance.negate();

        assertEquals(Money.of(-100.0, "USD"), result.principal());
        assertEquals(Money.of(-100.0, "USD"), result.interest());
        assertEquals(Money.of(-100.0, "USD"), result.fee());
        assertEquals(Money.of(-100.0, "USD"), result.excess());
    }

    @Test
    @DisplayName("Should calculate total amount correctly")
    void shouldCalculateTotalAmountCorrectly() {
        MonetaryAmount amount = Money.of(100.0, "USD");
        Balance balance = new Balance(amount, amount, amount, amount);

        MonetaryAmount result = balance.getTotalAmount();

        assertEquals(Money.of(400.0, "USD"), result);
    }

    @Test
    @DisplayName("Balance::get")
    void shouldGetBalanceComponent() {
        MonetaryAmount amount = Money.of(100.0, "USD");
        Balance balance = new Balance(amount, amount, amount, amount);

        assertEquals(amount, balance.get(BalanceComponent.PRINCIPAL));
        assertEquals(amount, balance.get(BalanceComponent.INTEREST));
        assertEquals(amount, balance.get(BalanceComponent.FEES));
        assertEquals(amount, balance.get(BalanceComponent.EXCESS));
    }
}