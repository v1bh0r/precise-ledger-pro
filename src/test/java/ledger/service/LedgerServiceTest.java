package ledger.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ledger.common.Ledger;
import ledger.common.LedgerActivityFactory;
import ledger.common.ledgeractivity.domain.InterestRate;
import ledger.common.ledgeractivity.temporalactivity.StartOfDay;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityContext;
import ledger.model.BalanceComponent;
import ledger.model.GeneralLedgerActivity;
import ledger.model.LedgerActivityImpactExpectation;
import ledger.model.LedgerEntry;
import ledger.repository.LedgerActivityRepository;
import ledger.util.CSVUtil;
import ledger.util.ObjectToCsvUtil;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ledger.common.MonetaryUtil.toDouble;
import static ledger.service.BalanceService.createZeroBalance;
import static org.junit.jupiter.api.Assertions.*;

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
    CSVUtil<LedgerActivityImpactExpectation> ledgerActivityImpactExpectationCSVUtil = new CSVUtil<>();
    CSVUtil<GeneralLedgerActivity> generalLedgerActivityCSVUtil = new CSVUtil<>();
    CSVUtil<InterestRate> interestRateCSVUtil = new CSVUtil<>();

    private static final String LOAN_ID = "1234";
    private static final String CURRENCY = "USD";
    private static final String DATA_PATH = "data/ledger/service/";

    @BeforeEach
    void setUp() {
        objectToCsvUtil = new ObjectToCsvUtil<>(log);
    }

    @AfterEach
    void tearDown() {
        objectToCsvUtil = null;
        ledgerActivityRepository.flush();
    }

    @Test
    void syncWithRetroactiveLedger_test1() throws IOException {
        // TODO: The test fails sometimes because of an issue with CSV parsing.
        //   I haven't been able to figure out why this happens and why only some times
        //   and not always.
        //   https://chat.openai.com/share/b3025f3c-e35b-45f4-a4b4-85767f579a93
        var ledger = initLedger("syncWithRetroactiveLedger_test1/ledger_entries.csv");
        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR +
                "syncWithRetroactiveLedger_test1_input1.csv");
        var retroactiveLedger = initLedger("syncWithRetroactiveLedger_test1/retroactive_ledger_entries.csv");
        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR +
                "syncWithRetroactiveLedger_test1_input2.csv");
        var currentTime = LocalDateTime.now();
        ledgerService.syncWithRetroactiveLedger(ledger, retroactiveLedger, currentTime, currentTime, 4);
        var expectedLedgerAfterSync = initLedger("syncWithRetroactiveLedger_test1/ledger_entries_after_sync.csv");

        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR +
                "syncWithRetroactiveLedger_test1_output.csv");
        assertEquals(expectedLedgerAfterSync.getEntries().size(), ledger.getEntries().size());
        assertEquals(expectedLedgerAfterSync.getCurrentBalance(), ledger.getCurrentBalance());
    }

    @Test
    void applyLedgerActivities_test1() throws IOException {
        // Setup

        final var temporalContext = getSampleTemporalActivityContext();

        var ledger = createEmptyLedger();
        var activities = generalLedgerActivityCSVUtil.parse(DATA_PATH + "applyLedgerActivities_test1" +
                "/ledger_activities.csv", GeneralLedgerActivity.class);
        var ledgerActivities = activities.stream().map(activity -> {
            var ledgerActivity = ledgerActivityFactory.create(activity, temporalContext);
            ledgerActivityRepository.insert(ledgerActivity);
            return ledgerActivity;
        }).toList();

        // Act
        ledgerService.applyLedgerActivities(ledger, ledgerActivities);

        // Write to CSV for debugging only
        // TODO: Delete this line before committing
        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR +
                "applyLedgerActivities_test1_ledger_entries.csv");

        // Assertions
        var expectedLedger = initLedger("applyLedgerActivities_test1/ledger_entries.csv");

        assertEquals(expectedLedger.getEntries().size(), ledger.getEntries().size());
        assertEquals(expectedLedger.getCurrentBalance(), ledger.getCurrentBalance());
    }

    private @NotNull TemporalActivityContext getSampleTemporalActivityContext() throws IOException {
        var interestRates = interestRateCSVUtil.parse(DATA_PATH + "applyLedgerActivities_test1/interest_rates.csv",
                InterestRate.class);
        var temporalContext = new TemporalActivityContext();
        temporalContext.setProperty("interestRates", interestRates);
        temporalContext.setProperty("daysInYear", 365);
        temporalContext.setProperty("currencyCode", CURRENCY);
        return temporalContext;
    }

    private static @NotNull Ledger createEmptyLedger() {
        return new Ledger(LOAN_ID, createZeroBalance(CURRENCY), new ArrayList<>(), CURRENCY);
    }

    @Test
    void testReverseLedgerActivity() throws IOException {
        // Setup
        var ledger = createEmptyLedger();
        var activities =
                generalLedgerActivityCSVUtil.parse(DATA_PATH + "testReverseLedgerActivity/ledger_activities" + ".csv"
                        , GeneralLedgerActivity.class);
        var ledgerActivities = activities.stream()
                .map(activity -> ledgerActivityFactory.create(activity, new TemporalActivityContext())).toList();
        ledgerActivities.forEach(ledgerActivityRepository::insert);
        ledgerService.applyLedgerActivities(ledger, ledgerActivities);

        // Verify
        var entries = ledger.getEntries();
        assertEquals(6, entries.size());
        assertEquals(990200.00, ledger.getCurrentBalance().principal().getNumber().doubleValue());
        assertEquals(0.00, ledger.getCurrentBalance().interest().getNumber().doubleValue());
        var impactOfSecondPayment = ledgerService.calculateTotalImpact(ledger, "Transaction", "345");
        assertEquals(-9800, impactOfSecondPayment.get(BalanceComponent.PRINCIPAL).getNumber().doubleValue());
        assertEquals(-200, impactOfSecondPayment.get(BalanceComponent.INTEREST).getNumber().doubleValue());
        var impactOfFirstPayment = ledgerService.calculateTotalImpact(ledger, "Transaction", "234");
        assertEquals(0, impactOfFirstPayment.getTotalAmount().getNumber().doubleValue());

        // Write to CSV for debugging only
        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR + "testReverseLedgerActivity.csv");
    }

    private @NotNull Ledger setupAndActForTestPastPayment(String x) throws IOException {
        var temporalContext = getSampleTemporalActivityContext();
        var ledger = createEmptyLedger();
        var activities = generalLedgerActivityCSVUtil.parse(DATA_PATH + x, GeneralLedgerActivity.class);
        var ledgerActivities = activities.stream()
                .map(activity -> ledgerActivityFactory.create(activity, temporalContext)).toList();
        ledgerActivities.forEach(ledgerActivityRepository::insert);

        // Act
        ledgerService.applyLedgerActivities(ledger, ledgerActivities);
        return ledger;
    }

    @Test
    void testPastDatedPayment() throws IOException {
        final var ledger = setupAndActForTestPastPayment("testPastDatedPayment/ledger_activities.csv");
        assertEquals(980000, ledger.getCurrentBalance().getTotalAmount().getNumber().doubleValue());
    }

    @Test
    void testPastDatedPayment2() throws IOException {
        final var ledger = setupAndActForTestPastPayment("testPastDatedPayment/ledger_activities2.csv");
        var impactExpectations = getImpactExpectations(DATA_PATH + "testPastDatedPayment" +
                "/ledger_activities2_expectation.csv");
        impactExpectations.forEach(expectation -> {
            var impact = expectation.getImpact();
            var actualImpact = ledgerService.calculateTotalImpact(ledger, expectation.activityType(),
                    expectation.activityId());
            assertTrue(impact.equals(actualImpact),
                    "Activity type: " + expectation.activityId() + " Activity Type: " + expectation.activityType() +
                            " " + "Expectation: " + expectation.getImpact() + " Actual: " + actualImpact);
        });
        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR + "testPastDatedPayment2.csv");
    }

    private List<LedgerActivityImpactExpectation> getImpactExpectations(String filePath) {
        try {
            return ledgerActivityImpactExpectationCSVUtil.parse(filePath, LedgerActivityImpactExpectation.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void testPastDatedPayment3() throws IOException {
        final var ledger = setupAndActForTestPastPayment("testPastDatedPayment/ledger_activities3.csv");
        assertEquals(990271.23, toDouble(ledger.getCurrentBalance().getTotalAmount()));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenReversingBackdatedEntry() throws IOException {
        // Setup
        var ledger = createEmptyLedger();
        var activities =
                generalLedgerActivityCSVUtil.parse(DATA_PATH + "testReverseLedgerActivity/ledger_activities" + ".csv"
                        , GeneralLedgerActivity.class);
        var ledgerActivities = activities.stream()
                .map(activity -> ledgerActivityFactory.create(activity, new TemporalActivityContext())).toList();
        ledgerActivities.forEach(ledgerActivityRepository::insert);
        ledgerService.applyLedgerActivities(ledger, ledgerActivities);

        var temporalContext = getSampleTemporalActivityContext();

        // Act
        assertThrows(IllegalArgumentException.class, () -> {
            var activity = new StartOfDay(LOAN_ID, "SOD", LocalDateTime.parse("2024-03-01T00:00:00"), temporalContext);
            activity.applyTo(ledger);
        });
    }

    private Ledger initLedger(String path) throws IOException {
        var ledgerEntries = ledgerEntryCSVUtil.parse(DATA_PATH + path, LedgerEntry.class);
        return new Ledger(LOAN_ID, createZeroBalance(CURRENCY), ledgerEntries, CURRENCY);
    }
}