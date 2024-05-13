package ledger.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import ledger.model.GeneralLedgerActivity;
import ledger.util.CSVUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class LedgerActivityRepositoryTest {
    LedgerActivityRepository ledgerActivityRepository;
    private final CSVUtil<GeneralLedgerActivity> generalLedgerActivityCSVUtil = new CSVUtil<>();
    private static final String DATA_PATH = "data/ledger/repository/ledgerActivityRepositoryTest/";

    @BeforeEach
    @Transactional
    void setUp() throws IOException {
        ledgerActivityRepository = new LedgerActivityRepository();
        ledgerActivityRepository.deleteAll();
        List<GeneralLedgerActivity> activities =
                generalLedgerActivityCSVUtil.parse(DATA_PATH + "ledger_activities" + ".csv",
                        GeneralLedgerActivity.class);
        activities.forEach(activity -> {
            ledgerActivityRepository.insert(activity);
        });
    }

    @Test
    @Transactional
    void deleteAllClearsPersistedActivities() {
        GeneralLedgerActivity activity1 = new GeneralLedgerActivity();
        GeneralLedgerActivity activity2 = new GeneralLedgerActivity();
        ledgerActivityRepository.insert(activity1);
        ledgerActivityRepository.insert(activity2);

        ledgerActivityRepository.deleteAll();

        List<GeneralLedgerActivity> activities = GeneralLedgerActivity.listAll();
        assertTrue(activities.isEmpty());
    }

    @Test
    @Transactional
    void getLedgerActivitiesCreatedSinceReturnsEmptyListWhenNoActivitiesExistButBeforeCreatedAt() {
        var results = ledgerActivityRepository.getLedgerActivitiesCreatedSinceButBeforeCreatedAt("nonexistent",
                "nonexistent", "nonexistent", LocalDateTime.now());
        assertTrue(results.isEmpty());
    }

    @Test
    @Transactional
    public void getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBeforeReturnsCorrectActivities() {
        String loanId = "testLoanId";
        LocalDateTime effectiveAt = LocalDateTime.now().minusDays(1);
        LocalDateTime createdAt = LocalDateTime.now();

        // Insert some test data
        GeneralLedgerActivity activity1 = GeneralLedgerActivity.builder()
                .loanId(loanId)
                .effectiveAt(effectiveAt.plusHours(1))
                .transactionTime(createdAt.minusHours(1)).build();
        ledgerActivityRepository.insert(activity1);

        GeneralLedgerActivity activity2 = GeneralLedgerActivity.builder()
                .loanId(loanId)
                .effectiveAt(effectiveAt.minusHours(1))
                .transactionTime(createdAt.minusHours(1)).build();
        ledgerActivityRepository.insert(activity2);

        List<GeneralLedgerActivity> actualActivities =
                ledgerActivityRepository.getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBefore(loanId,
                        effectiveAt, createdAt);

        assertEquals(1, actualActivities.size());
        assertEquals(activity1, actualActivities.getFirst());
    }

    @Test
    @Transactional
    public void getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBeforeReturnsEmptyWhenNoActivities() {
        String loanId = "testLoanId";
        LocalDateTime effectiveAt = LocalDateTime.now().minusDays(1);
        LocalDateTime createdAt = LocalDateTime.now();

        List<GeneralLedgerActivity> actualActivities =
                ledgerActivityRepository.getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBefore(loanId,
                        effectiveAt, createdAt);

        assertTrue(actualActivities.isEmpty());
    }

    @Test
    @Transactional
    public void getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBeforeReturnsActivitiesWhenEffectiveAtIsEqualToActivityEffectiveAt() {
        String loanId = "testLoanId";
        LocalDateTime effectiveAt = LocalDateTime.now().minusDays(1);
        LocalDateTime createdAt = LocalDateTime.now();

        GeneralLedgerActivity activity1 = GeneralLedgerActivity.builder()
                .loanId(loanId)
                .effectiveAt(effectiveAt) // This should be included in the result
                .transactionTime(createdAt.minusHours(1)).build();
        ledgerActivityRepository.insert(activity1);

        List<GeneralLedgerActivity> actualActivities =
                ledgerActivityRepository.getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBefore(loanId,
                        effectiveAt, createdAt);

        assertEquals(1, actualActivities.size());
        assertEquals(activity1, actualActivities.get(0));
    }
}