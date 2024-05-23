package ledger.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ledger.common.Ledger;
import ledger.common.LedgerActivityFactory;
import ledger.common.ledgeractivity.ReversalActivity;
import ledger.common.ledgeractivity.domain.InterestRate;
import ledger.common.ledgeractivity.domain.Loan;
import ledger.common.ledgeractivity.temporalactivity.StartOfDay;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityContext;
import ledger.model.*;
import ledger.repository.LedgerActivityRepository;
import ledger.util.CSVUtil;
import ledger.util.ObjectToCsvUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ledger.config.AppConfig.DEFAULT_CURRENCY_CODE;
import static ledger.config.AppConfig.DEFAULT_DAYS_IN_YEAR;
import static ledger.service.BalanceService.createZeroBalance;
import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;
import static ledger.util.MonetaryUtil.toDouble;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class LedgerServiceTest {
    private static final String BASE_TEST_OUTPUT_DIR = System.getProperty("user.dir") + "/test-output/";
    private ObjectToCsvUtil<LedgerEntry> objectToCsvUtil;
    @Inject
    LedgerService ledgerService;
    @Inject
    LoanService loanService;
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
    @Transactional
    void setUp() {
        objectToCsvUtil = new ObjectToCsvUtil<>();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        objectToCsvUtil = null;
        ledgerActivityRepository.deleteAll();
    }

    @Test
    @Transactional
    void syncWithRetroactiveLedger_test1() throws IOException, IllegalAccessException {
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
    @Transactional
    void applyLedgerActivities_test1() throws IOException {
        // Setup

        final var temporalContext = getSampleTemporalActivityContext();

        var ledger = createEmptyLedger();
        var activities = generalLedgerActivityCSVUtil.parse(DATA_PATH + "applyLedgerActivities_test1" +
                "/ledger_activities.csv", GeneralLedgerActivity.class);
        var ledgerActivities = activities.stream().map(activity -> {
            var ledgerActivity = ledgerActivityFactory.create(activity);
            ledgerActivityRepository.insert(activity);
            return ledgerActivity;
        }).toList();

        // Act
        ledgerService.applyLedgerActivities(ledger, ledgerActivities, new LedgerClock(), temporalContext);

        // Verify
        checkLedgerAgainstLedgerActivityImpactExpectations(ledger, DATA_PATH + "applyLedgerActivities_test1" +
                "/apply_ledger_activities_test1_expectation.csv");
    }

    private @NotNull TemporalActivityContext getSampleTemporalActivityContext() throws IOException {
        var interestRates = interestRateCSVUtil.parse(DATA_PATH + "applyLedgerActivities_test1/interest_rates.csv",
                InterestRate.class);
        var temporalContext = new TemporalActivityContext();
        temporalContext.setProperty("interestRates", interestRates);
        temporalContext.setProperty("daysInYear", DEFAULT_DAYS_IN_YEAR);
        temporalContext.setProperty("currencyCode", CURRENCY);
        return temporalContext;
    }

    private static @NotNull Ledger createEmptyLedger() {
        return new Ledger(LOAN_ID, createZeroBalance(CURRENCY), new ArrayList<>(), CURRENCY);
    }

    @Test
    @Transactional
    void testReverseLedgerActivity() throws IOException, IllegalAccessException {
        // Setup
        var ledger = createEmptyLedger();
        var activities =
                generalLedgerActivityCSVUtil.parse(DATA_PATH + "testReverseLedgerActivity/ledger_activities" + ".csv"
                        , GeneralLedgerActivity.class);
        var ledgerActivities = activities.stream().map(activity -> {
            ledgerActivityRepository.insert(activity);
            return ledgerActivityFactory.create(activity);
        }).toList();
        ledgerService.applyLedgerActivities(ledger, ledgerActivities, new LedgerClock(), new TemporalActivityContext());

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
        var ledgerActivities = activities.stream().map(activity -> {
            ledgerActivityRepository.insert(activity);
            return ledgerActivityFactory.create(activity);
        }).toList();

        // Act
        ledgerService.applyLedgerActivities(ledger, ledgerActivities, new LedgerClock(), temporalContext);
        return ledger;
    }

    @Test
    @Transactional
    void testPastDatedPayment() throws IOException {
        final var ledger = setupAndActForTestPastPayment("testPastDatedPayment/ledger_activities.csv");
        assertEquals(980000, ledger.getCurrentBalance().getTotalAmount().getNumber().doubleValue());
    }

    @Test
    @Transactional
    void testPastDatedPayment2() throws IOException {
        final var ledger = setupAndActForTestPastPayment("testPastDatedPayment/ledger_activities2.csv");

        checkLedgerAgainstLedgerActivityImpactExpectations(ledger, DATA_PATH + "testPastDatedPayment" +
                "/ledger_activities2_expectation.csv");
    }

    private void checkLedgerAgainstLedgerActivityImpactExpectations(Ledger ledger, String impactExpectationsPath) {
        var impactExpectations = getImpactExpectations(impactExpectationsPath);
        impactExpectations.forEach(expectation -> {
            var impact = expectation.getImpact();
            var actualImpact = ledgerService.calculateTotalImpact(ledger, expectation.activityType(),
                    expectation.activityId());
            assertTrue(impact.equals(actualImpact),
                    "Activity type: " + expectation.activityId() + " Activity Type: " + expectation.activityType() +
                            " " + "Expectation: " + expectation.getImpact() + " Actual: " + actualImpact);
        });
    }

    private List<LedgerActivityImpactExpectation> getImpactExpectations(String filePath) {
        try {
            return ledgerActivityImpactExpectationCSVUtil.parse(filePath, LedgerActivityImpactExpectation.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    @Transactional
    void testPastDatedPayment3() throws IOException {
        final var ledger = setupAndActForTestPastPayment("testPastDatedPayment/ledger_activities3.csv");
        assertEquals(990271.23, toDouble(ledger.getCurrentBalance().getTotalAmount()));
    }

    @Test
    @Transactional
    void shouldThrowIllegalArgumentExceptionWhenReversingBackdatedEntry() throws IOException {
        // Setup
        var ledger = createEmptyLedger();
        var activities =
                generalLedgerActivityCSVUtil.parse(DATA_PATH + "testReverseLedgerActivity/ledger_activities" + ".csv"
                        , GeneralLedgerActivity.class);
        var ledgerActivities = activities.stream().map(activity -> {
            ledgerActivityRepository.insert(activity);
            return ledgerActivityFactory.create(activity);
        }).toList();
        ledgerService.applyLedgerActivities(ledger, ledgerActivities, new LedgerClock(), new TemporalActivityContext());

        var temporalContext = getSampleTemporalActivityContext();

        // Act
        assertThrows(IllegalArgumentException.class, () -> {
            var activity = new StartOfDay(LOAN_ID, "SOD", LocalDateTime.parse("2024-03-01T00:00:00"));
            activity.applyTo(ledger, new LedgerClock(), temporalContext);
        });
    }

    @Test
    @Transactional
    void shouldThrowRuntimeExceptionWhenReversedActivityHasNoLedgerEntries() {
        // Setup
        var ledger = mock(Ledger.class);
        var reversalActivity = mock(ReversalActivity.class);
        when(reversalActivity.getReversedActivityType()).thenReturn("");
        when(reversalActivity.getReversedActivityId()).thenReturn("");

        // Act
        assertThrows(RuntimeException.class, () -> ledgerService.reverseLedgerActivity(reversalActivity, ledger,
                new LedgerClock(), new TemporalActivityContext()));
    }

    @Test
    @Transactional
    void shouldThrowRuntimeExceptionWhenReversedActivityHasNoLedgerEntries2() {
        // Setup
        var ledger = mock(Ledger.class);
        var reversalActivity = mock(ReversalActivity.class);
        when(reversalActivity.getReversedActivityType()).thenReturn("");
        when(reversalActivity.getReversedActivityId()).thenReturn("asdfasdfds");
        when(ledger.calculateTotalImpact("Asdfasdf", "asdfasdfds")).thenReturn(null);

        // Act
        assertThrows(RuntimeException.class, () -> ledgerService.reverseLedgerActivity(reversalActivity, ledger,
                new LedgerClock(), new TemporalActivityContext()));
    }

    @Test
    @Transactional
    void shouldThrowRuntimeExceptionWhenReversedActivityHasNoLedgerEntries3() {
        // Setup
        var ledger = mock(Ledger.class);
        var reversalActivity = mock(ReversalActivity.class);
        when(reversalActivity.getReversedActivityType()).thenReturn("Asdfasdf");
        when(reversalActivity.getReversedActivityId()).thenReturn("");

        // Act
        assertThrows(RuntimeException.class, () -> ledgerService.reverseLedgerActivity(reversalActivity, ledger,
                new LedgerClock(), new TemporalActivityContext()));
    }

    @Test
    @Transactional
    void whenLedgerHasAFreezeDateAndAStartingBalance() {
        // Setup
        var loanCutOff = LocalDateTime.now().minusDays(30);
        var loan = Loan.builder().lastLedgerFrozenOn(loanCutOff).lastLedgerFreezePrincipalBalance(1000.0)
                .lastLedgerFreezeInterestBalance(200.0).lastLedgerFreezeFeeBalance(300.0)
                .lastLedgerFreezeExcessBalance(0.0).build();

        loan.persist();

        var interestRate = InterestRate.builder().loanId(loan.getId().toString()).rate(0.1f).effectiveAt(loanCutOff)
                .build();

        interestRate.persist();

        var today = loanCutOff.plusDays(7);
        // Act
        var generalActivity = GeneralLedgerActivity.builder().loanId(LOAN_ID).activityType("Transaction")
                .activityId("1").commonName("Disbursal").direction("CREDIT").transactionStrategy("ComputationalSpread")
                .amount(2000.0).spread("P").effectiveAt(today).transactionTime(today).build();

        var ledger = ledgerService.getLedger(loan.getId());
        ledgerService.applyLedgerActivities(ledger, List.of(ledgerActivityFactory.create(generalActivity)),
                ledgerService.getCurrentLedgerClock(ledger), loanService.getTemporalActivityContext(loan.getId()));

        // Verify
        assertEquals(3000.0, toDouble(ledger.getCurrentBalance().principal()));
    }

    @Test
    @Transactional
        // precise-ledger-pro-20
    void bug_preciseLedgerPro_20() throws IOException, IllegalAccessException {
        // Setup
        var interestRates = List.of(InterestRate.builder().loanId(LOAN_ID).rate(0.1845f)
                .effectiveAt(DB_SAFE_LOCAL_DATETIME_MIN).build());
        var temporalContext = new TemporalActivityContext();
        temporalContext.setProperty("interestRates", interestRates);
        temporalContext.setProperty("daysInYear", DEFAULT_DAYS_IN_YEAR);
        temporalContext.setProperty("currencyCode", CURRENCY);

        var ledger = createEmptyLedger();
        var activities = generalLedgerActivityCSVUtil.parse(DATA_PATH + "bug/precise-ledger-pro-20/ledger-activities" +
                ".csv", GeneralLedgerActivity.class);
        var ledgerActivities = activities.stream().map(activity -> {
            var ledgerActivity = ledgerActivityFactory.create(activity);
            ledgerActivityRepository.insert(activity);
            return ledgerActivity;
        }).toList();

        // Act
        ledgerService.applyLedgerActivities(ledger, ledgerActivities, new LedgerClock(DB_SAFE_LOCAL_DATETIME_MIN),
                temporalContext);

        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR + "bug_preciseLedgerPro_20.csv");

        // Verify
        checkLedgerAgainstLedgerActivityImpactExpectations(ledger, DATA_PATH + "bug/precise-ledger-pro-20" +
                "/expectations.csv");
    }

    private Ledger initLedger(String path) throws IOException {
        var ledgerEntries = ledgerEntryCSVUtil.parse(DATA_PATH + path, LedgerEntry.class);
        return new Ledger(LOAN_ID, createZeroBalance(DEFAULT_CURRENCY_CODE), ledgerEntries, DEFAULT_CURRENCY_CODE);
    }
}