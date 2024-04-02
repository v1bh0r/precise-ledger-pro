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
    StaticSpread staticSpread;

    @BeforeEach
    void setUp() {
        balanceService = new BalanceService(monetaryAmountFactory);
        balance = balanceService.createBalance(100, 10, 5, 0, "USD");
        amountAllocation = balanceService.createBalance(50, 5, 2.5, 0, "USD");
        staticSpread = new StaticSpread(amountAllocation, Direction.CREDIT);
        staticSpread.amountAllocation = amountAllocation;
    }

    @Test
    void applyTo_increase() {
        staticSpread.direction = Direction.CREDIT;
        Balance expectedBalance = balanceService.createBalance(150, 15, 7.5, 0, "USD");

        Balance result = staticSpread.applyTo(balance);

        assertEquals(expectedBalance, result);
    }

    @Test
    void applyTo_decrease() {
        staticSpread.direction = Direction.DEBIT;
        Balance expectedBalance = balanceService.createBalance(50, 5, 2.5, 0, "USD");

        Balance result = staticSpread.applyTo(balance);

        assertEquals(expectedBalance, result);
    }
}