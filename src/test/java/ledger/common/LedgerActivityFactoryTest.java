package ledger.common;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ledger.common.ledgeractivity.ReversalActivity;
import ledger.common.ledgeractivity.temporalactivity.StartOfDay;
import ledger.common.ledgeractivity.transactionactivity.Transaction;
import ledger.model.GeneralLedgerActivity;
import ledger.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static ledger.service.BalanceService.createBlankLedger;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class LedgerActivityFactoryTest {

    @Inject
    LedgerService ledgerService;
    private LedgerActivityFactory ledgerActivityFactory;

    private final String LOAN_ID = "1234";

    @BeforeEach
    void setUp() {
        ledgerActivityFactory = new LedgerActivityFactory(ledgerService);
    }

    @Test
    void testCreateTransaction() {
        GeneralLedgerActivity generalActivity = GeneralLedgerActivity.builder()
                .loanId(LOAN_ID)
                .activityType("Transaction")
                .activityId("1")
                .direction("DEBIT")
                .transactionStrategy("ComputationalSpread")
                .spread("IPF")
                .effectiveAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();


        LedgerActivity result = ledgerActivityFactory.create(createBlankLedger(LOAN_ID), generalActivity, null);

        assertInstanceOf(Transaction.class, result);
    }

    @Test
    void testCreateStartOfDay() {
        GeneralLedgerActivity generalActivity = GeneralLedgerActivity.builder()
                .loanId(LOAN_ID)
                .activityId("1")
                .activityType("StartOfDay")
                .effectiveAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        var ledger = createBlankLedger(LOAN_ID);
        LedgerActivity result = ledgerActivityFactory.create(ledger, generalActivity, null);

        assertInstanceOf(StartOfDay.class, result);
    }

    @Test
    void testCreateReversal() {
        var generalActivity = GeneralLedgerActivity.builder()
                .loanId("123")
                .activityId("1")
                .activityType("Reversal")
                .reversalActivityType("Transaction")
                .effectiveAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .reversalActivityId("1")
                .build();

        var ledger = createBlankLedger(LOAN_ID);
        LedgerActivity result = ledgerActivityFactory.create(ledger, generalActivity, null);
        assertInstanceOf(ReversalActivity.class, result);
    }

    @Test
    void testCreateUnknownActivityType() {
        GeneralLedgerActivity generalActivity = GeneralLedgerActivity.builder()
                .activityType("Unknown")
                .build();

        var ledger = createBlankLedger(LOAN_ID);
        LedgerActivity result = ledgerActivityFactory.create(ledger, generalActivity, null);

        assertNull(result);
    }

}