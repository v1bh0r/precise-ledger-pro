package ledger.common.ledgeractivity.transactionactivity.transactionspreadstrategy;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ledger.model.Balance;
import ledger.model.Direction;
import ledger.service.BalanceService;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.money.MonetaryAmountFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class StaticSpreadTest {
    @Inject
    BalanceService balanceService;
    @Inject
    MonetaryAmountFactory<Money> monetaryAmountFactory;
    Balance balance;
    Balance amountAllocation;

    @BeforeEach
    void setUp() {
        balanceService = new BalanceService(monetaryAmountFactory);
        balance = balanceService.createBalance(100, 10, 5, 0, "USD");
        amountAllocation = balanceService.createBalance(50, 5, 2.5, 0, "USD");
    }

    @Test
    void applyTo_increase() {
        var staticSpread = new StaticSpread(amountAllocation, Direction.CREDIT);
        Balance expectedBalance = balanceService.createBalance(150, 15, 7.5, 0, "USD");

        Balance result = staticSpread.applyTo(balance);

        assertEquals(expectedBalance, result);
    }

    @Test
    void applyTo_decrease() {
        var staticSpread = new StaticSpread(amountAllocation, Direction.DEBIT);
        Balance expectedBalance = balanceService.createBalance(50, 5, 2.5, 0, "USD");

        Balance result = staticSpread.applyTo(balance);

        assertEquals(expectedBalance, result);
    }
}