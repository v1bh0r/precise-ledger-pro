package ledger.common.ledgeractivity.transactionactivity.transactionspreadstrategy;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ledger.model.Balance;
import ledger.model.BalanceComponent;
import ledger.model.Direction;
import ledger.service.BalanceService;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.money.MonetaryAmountFactory;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ComputationalSpreadTest {
    @Inject
    BalanceService balanceService;
    @Inject
    MonetaryAmountFactory<Money> monetaryAmountFactory;
    Balance balance;

    @BeforeEach
    void setUp() {
        balanceService = new BalanceService(monetaryAmountFactory);
        balance = balanceService.createBalance(100, 10, 5, 0, "USD");
    }

    @Test
    void applyTo_increase() {
        var computationalSpread = new ComputationalSpread(Money.of(50, "USD"), Direction.CREDIT);
        Balance expectedBalance = balanceService.createBalance(100, 10, 55, 0, "USD");
        Balance result = computationalSpread.applyTo(balance);
        assertEquals(expectedBalance, result);
    }

    @Test
    void applyTo_increasePrincipal() {
        var computationalSpread = new ComputationalSpread(Money.of(50, "USD"), Direction.CREDIT,
                new LinkedHashSet<>(List.of(BalanceComponent.PRINCIPAL)));
        Balance expectedBalance = balanceService.createBalance(150, 10, 5, 0, "USD");
        Balance result = computationalSpread.applyTo(balance);
        assertEquals(expectedBalance, result);
    }

    @Test
    void applyTo_decrease() {
        var computationalSpread = new ComputationalSpread(Money.of(50, "USD"), Direction.DEBIT);
        Balance expectedBalance = balanceService.createBalance(65, 0, 0, 0, "USD");
        Balance result = computationalSpread.applyTo(balance);
        assertEquals(expectedBalance, result);
    }

    @Test
    void applyTo_decrease_havingExcess() {
        var computationalSpread = new ComputationalSpread(Money.of(150, "USD"), Direction.DEBIT);
        Balance expectedBalance = balanceService.createBalance(0, 0, 0, 35, "USD");
        Balance result = computationalSpread.applyTo(balance);
        assertEquals(expectedBalance, result);
    }

    @Test
    void applyTo_whenCreditingToBalanceHavingExcess_FirstReduceExcessBeforeIncreasingOtherComponents() {
        var startingBalance = balanceService.createBalance(0, 0, 0, 35, "USD");
        var computationalSpread = new ComputationalSpread(Money.of(100, "USD"), Direction.CREDIT, "P");
        Balance expectedBalance = balanceService.createBalance(65, 0, 0, 0, "USD");
        Balance result = computationalSpread.applyTo(startingBalance);
        assertEquals(expectedBalance, result);
    }

    @Test
    void applyTo_whenCreditingToBalanceHavingExcess_FirstReduceExcessBeforeIncreasingOtherInterest() {
        var startingBalance = balanceService.createBalance(0, 0, 0, 35, "USD");
        var computationalSpread = new ComputationalSpread(Money.of(100, "USD"), Direction.CREDIT, "I");
        Balance expectedBalance = balanceService.createBalance(0, 65, 0, 0, "USD");
        Balance result = computationalSpread.applyTo(startingBalance);
        assertEquals(expectedBalance, result);
    }
}