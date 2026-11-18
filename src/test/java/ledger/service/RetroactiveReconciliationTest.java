package ledger.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ledger.common.Ledger;
import ledger.common.LedgerActivityFactory;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityContext;
import ledger.model.Balance;
import ledger.model.GeneralLedgerActivity;
import ledger.model.LedgerActivityImpactExpectation;
import ledger.model.LedgerClock;
import ledger.model.LedgerEntry;
import ledger.repository.LedgerActivityRepository;
import ledger.util.CSVUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static ledger.service.BalanceService.createZeroBalance;
import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Known failures in retroactive reconciliation, as fixtures under {@code bug/<scenario>/}. Each
 * {@code expectations.csv} holds what the same activities give when processed in business order, where no
 * backdated or reversal path is needed. The API-level counterpart is {@code RetroactiveReconciliationE2ETest}.
 */
@QuarkusTest
class RetroactiveReconciliationTest {

    private static final String DATA_PATH = "data/ledger/service/bug/";
    private static final String LOAN_ID = "1234";
    private static final String CURRENCY = "USD";

    @Inject
    LedgerService ledgerService;
    @Inject
    LedgerActivityFactory ledgerActivityFactory;
    @Inject
    LedgerActivityRepository ledgerActivityRepository;

    @AfterEach
    @Transactional
    void tearDown() {
        ledgerActivityRepository.deleteAll();
    }

    @Test
    @Transactional
    void doubleReversal() throws IOException {
        var ledger = replay("double-reversal");
        checkImpacts(ledger, "double-reversal");
    }

    @Test
    @Transactional
    void backdatedPaymentStaleFork() throws IOException {
        var ledger = replay("backdated-payment-stale-fork");
        checkImpacts(ledger, "backdated-payment-stale-fork");
        checkEntriesAddUpToBalance(ledger);
    }

    /** Applies the scenario's activities in the order they are listed, which is their arrival order. */
    private Ledger replay(String scenario) throws IOException {
        var ledger = new Ledger(LOAN_ID, createZeroBalance(CURRENCY), new ArrayList<>(), CURRENCY);
        var activities = new CSVUtil<GeneralLedgerActivity>().parse(DATA_PATH + scenario + "/ledger-activities.csv",
                GeneralLedgerActivity.class);
        var ledgerActivities = activities.stream().map(activity -> {
            ledgerActivityRepository.insert(activity);
            return ledgerActivityFactory.create(activity);
        }).toList();
        ledgerService.applyLedgerActivities(ledger, ledgerActivities, new LedgerClock(DB_SAFE_LOCAL_DATETIME_MIN),
                new TemporalActivityContext());
        return ledger;
    }

    private void checkImpacts(Ledger ledger, String scenario) throws IOException {
        var expectations = new CSVUtil<LedgerActivityImpactExpectation>().parse(
                DATA_PATH + scenario + "/expectations.csv", LedgerActivityImpactExpectation.class);
        for (var expectation : expectations) {
            var actual = ledgerService.calculateTotalImpact(ledger, expectation.activityType(), expectation.activityId());
            assertTrue(expectation.getImpact().equals(actual), scenario + ": activity " + expectation.activityId()
                    + " expected impact " + expectation.getImpact() + " but was " + actual);
        }
    }

    /** The reported balance must equal the sum of every entry's change. */
    private static void checkEntriesAddUpToBalance(Ledger ledger) {
        var sum = ledger.getEntries().stream().map(LedgerEntry::getBalanceChange).reduce(Balance::add).orElseThrow();
        var balance = ledger.getCurrentBalance();
        assertTrue(sum.equals(balance), "sum of entries " + sum + " differs from the current balance " + balance);
    }
}
