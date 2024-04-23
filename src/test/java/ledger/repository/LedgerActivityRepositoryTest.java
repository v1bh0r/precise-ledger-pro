package ledger.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ledger.common.LedgerActivityFactory;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityContext;
import ledger.model.GeneralLedgerActivity;
import ledger.util.CSVUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class LedgerActivityRepositoryTest {
    private final String LOAN_ID = "1234";

    @Inject
    LedgerActivityFactory ledgerActivityFactory;
    LedgerActivityRepository ledgerActivityRepository;
    private final CSVUtil<GeneralLedgerActivity> generalLedgerActivityCSVUtil = new CSVUtil<>();
    private static final String DATA_PATH = "data/ledger/repository/ledgerActivityRepositoryTest/";

    @BeforeEach
    void setUp() throws IOException {
        ledgerActivityRepository = new LedgerActivityRepository(ledgerActivityFactory);
        List<GeneralLedgerActivity> activities = generalLedgerActivityCSVUtil.parse(DATA_PATH + "ledger_activities.csv", GeneralLedgerActivity.class);
        var temporaryContext = new TemporalActivityContext();
        activities.forEach(activity -> {
            activity.setLoanId(LOAN_ID);
            ledgerActivityRepository.insert(activity, temporaryContext);
        });
    }

    @Test
    void findFirstByLoanIdAndTypeAndIdReturnsCorrectActivity() throws IOException {
        var firstActivity = ledgerActivityRepository.getLedgerActivities().getFirst();
        var result = ledgerActivityRepository.findFirstByLoanIdAndTypeAndId(firstActivity.getLoanId(), firstActivity.getActivityType(), firstActivity.getActivityId());
        assertEquals(firstActivity, result);
    }

    @Test
    void findFirstByLoanIdAndTypeAndIdReturnsCorrectActivityWhenExists() {
        var firstActivity = ledgerActivityRepository.getLedgerActivities().getFirst();
        var result = ledgerActivityRepository.findFirstByLoanIdAndTypeAndId(firstActivity.getLoanId(), firstActivity.getActivityType(), firstActivity.getActivityId());
        assertEquals(firstActivity, result);
    }

    @Test
    void findFirstByLoanIdAndTypeAndIdReturnsNullWhenDoesNotExist() {
        var result = ledgerActivityRepository.findFirstByLoanIdAndTypeAndId("nonexistent", "nonexistent", "nonexistent");
        assertNull(result);
    }

    @Test
    void findByLoanIdAndCreatedAfterReturnsActivitiesWhenTheyExist() {
        var firstActivity = ledgerActivityRepository.getLedgerActivities().getFirst();
        var results = ledgerActivityRepository.findByLoanIdAndCreatedAfter(firstActivity);
        assertFalse(results.isEmpty());
        results.forEach(result -> {
            assertTrue(result.getCreatedAt().isAfter(firstActivity.getCreatedAt()) || result.getCreatedAt().isEqual(firstActivity.getCreatedAt()));
            assertEquals(firstActivity.getLoanId(), result.getLoanId());
        });
    }

    @Test
    void findByLoanIdAndCreatedAfterReturnsEmptyListWhenNoActivitiesExist() {
        var lastActivity = ledgerActivityRepository.getLedgerActivities().getLast();
        var results = ledgerActivityRepository.findByLoanIdAndCreatedAfter(lastActivity);
        assertTrue(results.isEmpty());
    }

    @Test
    void getLedgerActivitiesCreatedSinceReturnsActivitiesWhenTheyExist() {
        var firstActivity = ledgerActivityRepository.getLedgerActivities().getFirst();
        var results = ledgerActivityRepository.getLedgerActivitiesCreatedSince(firstActivity.getLoanId(), firstActivity.getActivityType(), firstActivity.getActivityId());
        assertEquals(8, results.size());
        assertEquals("StartOfDay", results.getFirst().getActivityType());
    }

    @Test
    void getLedgerActivitiesCreatedSinceReturnsEmptyListWhenNoActivitiesExist() {
        var results = ledgerActivityRepository.getLedgerActivitiesCreatedSince("nonexistent", "nonexistent", "nonexistent");
        assertTrue(results.isEmpty());
    }
}