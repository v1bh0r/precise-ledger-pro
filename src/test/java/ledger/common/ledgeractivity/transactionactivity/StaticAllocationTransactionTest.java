package ledger.common.ledgeractivity.transactionactivity;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ledger.common.Ledger;
import ledger.model.Balance;
import ledger.model.Direction;
import ledger.model.LedgerClock;
import ledger.service.BalanceService;
import ledger.service.LedgerService;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import javax.money.Monetary;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static ledger.common.MonetaryUtil.toMonetaryAmount;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class StaticAllocationTransactionTest {
    private static final String CURRENCY = "USD";
    @Inject
    BalanceService balanceService;

    @Inject
    LedgerService ledgerService;

    @Inject
    Logger log;

    private static final String LOAN_ID = "12345";
    private static final String ACTIVITY_TYPE = "StaticAllocation";
    private static final String ACTIVITY_ID = "1";

    private Ledger createLedger(Balance startingBalance) {
        return new Ledger(LOAN_ID, startingBalance, new ArrayList<>(), CURRENCY);
    }

    @Test
    void applyTo() {
        var ledger = createLedger(balanceService.createBalance(10000, 100, 10, 0, CURRENCY));
        var customSpreadOverride = balanceService.createBalance(5000, 50, 5, 0, CURRENCY);
        var staticAllocationTransaction = new StaticAllocationTransaction(LOAN_ID, "Adjustment", ACTIVITY_TYPE,
                ACTIVITY_ID, customSpreadOverride, Direction.CREDIT, LocalDateTime.now(), LocalDateTime.now(),
                ledgerService);
        staticAllocationTransaction.applyTo(ledger, new LedgerClock());
        // Expect an entry to be added to the ledger having static allocation transaction
        var entries = ledger.getEntries();
        assertEquals(1, entries.size());
        var entry = entries.getFirst();
        var balance = entry.getBalance();
        assertEquals(toMonetaryAmount(15000), balance.principal());
        assertEquals(toMonetaryAmount(150), balance.interest().with(Monetary.getDefaultRounding()));
        ledger.log(log);
    }
}