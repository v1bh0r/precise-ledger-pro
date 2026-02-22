package ledger.api.ledgerevent;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import ledger.model.LedgerActivityImpactExpectation;
import ledger.service.LedgerService;
import ledger.util.CSVUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E test that applies ledger activities one-by-one via the single-activity REST endpoint,
 * mirroring the behavior of the web UI (LoanLifeCycleSimulation.vue).
 * <p>
 * This verifies that the API path produces the same results as the unit test
 * ({@code LedgerServiceTest.applyLedgerActivities_test1}) which applies all activities in-memory at once.
 */
@QuarkusTest
public class LedgerActivityE2ETest {

    private static final String DATA_PATH = "data/ledger/service/applyLedgerActivities_test1/";
    private final CSVUtil<LedgerActivityImpactExpectation> impactExpectationCSVUtil = new CSVUtil<>();

    @Inject
    LedgerService ledgerService;

    @Test
    public void applyActivitiesOneByOne_shouldMatchExpectedImpacts() throws IOException {
        // 1. Create a loan
        UUID loanId = createLoan();

        // 2. Create the interest rate (10% effective from 2024-03-01)
        createInterestRate(loanId, "2024-03-01T00:00:00", 0.10f);

        // 3. Apply each activity one-by-one via the single-activity API, same as the UI does
        // Activity 1: Disbursal
        postActivity(loanId, activityJson("123", "Disbursal", "Transaction", "ComputationalSpread",
                "CREDIT", "P", null, null, 1000000.0,
                "2024-03-01T00:00:00", "2024-03-01T00:00:00"));

        // Activity 2: SOD 2024-03-02
        postActivity(loanId, activityJson("20240302", "SOD", "StartOfDay", null,
                null, null, null, null, 0.0,
                "2024-03-02T00:00:00", "2024-03-02T00:00:00"));

        // Activity 3: SOD 2024-03-03
        postActivity(loanId, activityJson("20240303", "SOD", "StartOfDay", null,
                null, null, null, null, 0.0,
                "2024-03-03T00:00:00", "2024-03-03T00:00:00"));

        // Activity 4: Payment $10,000
        postActivity(loanId, activityJson("234", "Payment", "Transaction", "ComputationalSpread",
                "DEBIT", "IPF", null, null, 10000.0,
                "2024-03-03T13:00:00", "2024-03-03T13:00:00"));

        // Activity 5: SOD 2024-03-04
        postActivity(loanId, activityJson("20240304", "SOD", "StartOfDay", null,
                null, null, null, null, 0.0,
                "2024-03-04T00:00:00", "2024-03-04T00:00:00"));

        // Activity 6: SOD 2024-03-05
        postActivity(loanId, activityJson("20240305", "SOD", "StartOfDay", null,
                null, null, null, null, 0.0,
                "2024-03-05T00:00:00", "2024-03-05T00:00:00"));

        // Activity 7: Payment $5,000
        postActivity(loanId, activityJson("456", "Payment", "Transaction", "ComputationalSpread",
                "DEBIT", "IPF", null, null, 5000.0,
                "2024-03-05T10:00:00", "2024-03-05T10:00:00"));

        // Activity 8: Reversal of Payment 234
        postActivity(loanId, activityJson("123", "Reversal - Payment P123", "Reversal", null,
                null, null, "Transaction", "234", 0.0,
                "2024-03-05T13:00:00", "2024-03-05T13:00:00"));

        // Activity 9: Backdated Payment $4,000 (effectiveAt is in the past but transactionTime is later)
        postActivity(loanId, activityJson("768", "Payment", "Transaction", "ComputationalSpread",
                "DEBIT", "IPF", null, null, 4000.0,
                "2024-03-03T14:00:00", "2024-03-05T15:00:00"));

        // 4. Fetch the ledger via service layer (entries loaded from DB with ORDER BY)
        //    and verify impacts match the same expectations used by the unit test
        var ledger = ledgerService.getLedger(loanId);

        var expectations = impactExpectationCSVUtil.parse(
                DATA_PATH + "apply_ledger_activities_test1_expectation.csv",
                LedgerActivityImpactExpectation.class);

        for (var expectation : expectations) {
            var expectedImpact = expectation.getImpact();
            var actualImpact = ledger.calculateTotalImpact(expectation.activityType(),
                    expectation.activityId());
            assertTrue(expectedImpact.equals(actualImpact),
                    "Activity Id: " + expectation.activityId() +
                            " Activity Type: " + expectation.activityType() +
                            " Expected: " + expectedImpact +
                            " Actual: " + actualImpact);
        }
    }

    private UUID createLoan() {
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "daysInYear", 365,
                        "currencyCode", "USD",
                        "externalId", "",
                        "lastLedgerFreezePrincipalBalance", 0.0,
                        "lastLedgerFreezeInterestBalance", 0.0,
                        "lastLedgerFreezeFeeBalance", 0.0,
                        "lastLedgerFreezeExcessBalance", 0.0,
                        "lastLedgerFrozenOn", DB_SAFE_LOCAL_DATETIME_MIN.toString()
                ))
                .post("/api/v1/loans");
        response.then().statusCode(200);
        return response.body().jsonPath().getUUID("id");
    }

    private void createInterestRate(UUID loanId, String effectiveAt, float rate) {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "loanId", loanId.toString(),
                        "rate", rate,
                        "effectiveAt", effectiveAt
                ))
                .post("/api/v1/interest-rates")
                .then()
                .statusCode(201);
    }

    private void postActivity(UUID loanId, String activityJson) {
        given()
                .contentType(ContentType.JSON)
                .pathParam("loanId", loanId)
                .body(activityJson)
                .post("/api/v1/loans/{loanId}/ledger-activities")
                .then()
                .statusCode(200);
    }

    /**
     * Builds a JSON string for a GeneralLedgerActivity, ensuring all nullable Double fields have defaults.
     */
    private String activityJson(String activityId, String commonName, String activityType,
                                String transactionStrategy, String direction, String spread,
                                String reversalActivityType, String reversalActivityId,
                                double amount, String effectiveAt, String transactionTime) {
        var sb = new StringBuilder("{");
        sb.append("\"activityId\":\"").append(activityId).append("\",");
        sb.append("\"commonName\":\"").append(commonName).append("\",");
        sb.append("\"activityType\":\"").append(activityType).append("\",");
        if (transactionStrategy != null)
            sb.append("\"transactionStrategy\":\"").append(transactionStrategy).append("\",");
        if (direction != null)
            sb.append("\"direction\":\"").append(direction).append("\",");
        if (spread != null)
            sb.append("\"spread\":\"").append(spread).append("\",");
        if (reversalActivityType != null)
            sb.append("\"reversalActivityType\":\"").append(reversalActivityType).append("\",");
        if (reversalActivityId != null)
            sb.append("\"reversalActivityId\":\"").append(reversalActivityId).append("\",");
        sb.append("\"amount\":").append(amount).append(",");
        sb.append("\"principal\":0.0,");
        sb.append("\"interest\":0.0,");
        sb.append("\"fee\":0.0,");
        sb.append("\"excess\":0.0,");
        sb.append("\"effectiveAt\":\"").append(effectiveAt).append("\",");
        sb.append("\"transactionTime\":\"").append(transactionTime).append("\"");
        sb.append("}");
        return sb.toString();
    }
}
