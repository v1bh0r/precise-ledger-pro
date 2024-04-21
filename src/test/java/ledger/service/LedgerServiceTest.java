package ledger.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ledger.common.Ledger;
import ledger.common.LedgerActivity;
import ledger.common.LedgerActivityFactory;
import ledger.common.ledgeractivity.domain.InterestRate;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityContext;
import ledger.model.GeneralLedgerActivity;
import ledger.model.LedgerEntry;
import ledger.repository.LedgerActivityRepository;
import ledger.util.CSVUtil;
import ledger.util.ObjectToCsvUtil;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static ledger.service.BalanceService.createZeroBalance;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class LedgerServiceTest {
    private static final String BASE_TEST_OUTPUT_DIR = System.getProperty("user.dir") + "/test-output/";
    private ObjectToCsvUtil<LedgerEntry> objectToCsvUtil;
    @Inject
    LedgerService ledgerService;
    @Inject
    Logger log;

    @Inject
    LedgerActivityFactory ledgerActivityFactory;

    @Inject
    LedgerActivityRepository ledgerActivityRepository;
    CSVUtil<LedgerEntry> ledgerEntryCSVUtil = new CSVUtil<>();
    CSVUtil<GeneralLedgerActivity> generalLedgerActivityCSVUtil = new CSVUtil<>();
    CSVUtil<InterestRate> interestRateCSVUtil = new CSVUtil<>();

    private static final String LOAN_ID = "1234";
    private static final String CURRENCY = "USD";
    private static final String DATA_PATH = "data/ledger/service/";

    @BeforeEach
    void setUp() {
        objectToCsvUtil = new ObjectToCsvUtil<>(log);
    }

    @Test
    void syncWithRetroactiveLedger_test1() throws IOException {
        // TODO: The test fails sometimes because of an issue with CSV parsing.
        //   I haven't been able to figure out why this happens and why only some times
        //   and not always.
        //   https://chat.openai.com/share/b3025f3c-e35b-45f4-a4b4-85767f579a93
        var ledger = initLedger("syncWithRetroactiveLedger_test1/ledger_entries.csv");
        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR + "syncWithRetroactiveLedger_test1_input1.csv");
        var retroactiveLedger = initLedger("syncWithRetroactiveLedger_test1/retroactive_ledger_entries.csv");
        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR + "syncWithRetroactiveLedger_test1_input2.csv");
        var currentTime = LocalDateTime.now();
        ledgerService.syncWithRetroactiveLedger(ledger, retroactiveLedger, currentTime, 4);
        var expectedLedgerAfterSync = initLedger("syncWithRetroactiveLedger_test1/ledger_entries_after_sync.csv");

        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR + "syncWithRetroactiveLedger_test1_output.csv");
        assertEquals(expectedLedgerAfterSync.getEntries().size(), ledger.getEntries().size());
        assertEquals(expectedLedgerAfterSync.getCurrentBalance(), ledger.getCurrentBalance());
    }

    @Test
    void applyLedgerActivities_test1() throws IOException {
        // Setup

        var interestRates = interestRateCSVUtil.parse(DATA_PATH + "applyLedgerActivities_test1/interest_rates.csv", InterestRate.class);
        var temporalContext = new TemporalActivityContext();
        temporalContext.setProperty("interestRates", interestRates);
        temporalContext.setProperty("daysInYear", 365);
        temporalContext.setProperty("currencyCode", CURRENCY);

        var ledger = new Ledger(LOAN_ID, createZeroBalance(CURRENCY), new ArrayList<>(), CURRENCY);
        var activities = generalLedgerActivityCSVUtil.parse(DATA_PATH + "applyLedgerActivities_test1/ledger_activities.csv", GeneralLedgerActivity.class);
        var ledgerActivities = activities.stream().map(activity -> {
            activity.setLoanId(LOAN_ID);
            return ledgerActivityFactory.create(ledger, activity, temporalContext);
        }).toList();

        // Execution - Build the ledger from ledger activities
        for (LedgerActivity activity : ledgerActivities) {
            activity.applyTo(ledger);
        }

        // Write to CSV for debugging only
        // TODO: Delete this line before committing
        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR + "applyLedgerActivities_test1_ledger_entries.csv");

        // Assertions
        var expectedLedger = initLedger("applyLedgerActivities_test1/ledger_entries.csv");

        assertEquals(expectedLedger.getEntries().size(), ledger.getEntries().size());
        assertEquals(expectedLedger.getCurrentBalance(), ledger.getCurrentBalance());
    }

    private Ledger initLedger(String path) throws IOException {
        var ledgerEntries = ledgerEntryCSVUtil.parse(DATA_PATH + path, LedgerEntry.class);
        return new Ledger(LOAN_ID, createZeroBalance(CURRENCY), ledgerEntries, CURRENCY);
    }

    @Test
    void reverseLedgerActivity() {
        throw new RuntimeException("Unimplemented");
    }

    @Test
    void testReverseLedgerActivity() {
        throw new RuntimeException("Unimplemented");
    }
}