package ledger.api.ledgerevent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import ledger.model.Balance;
import ledger.model.LedgerActivityImpactExpectation;
import ledger.model.LedgerEntry;
import ledger.service.LedgerService;
import ledger.util.CSVUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fixtures of {@code RetroactiveReconciliationTest}, posted one activity at a time through the
 * single-activity API, as the web UI does. On this path the ledger clock is rebuilt from the stored entries
 * before each activity ({@code LedgerService.getCurrentLedgerClock}).
 */
@QuarkusTest
public class RetroactiveReconciliationE2ETest {

    private static final String DATA_PATH = "data/ledger/service/bug/";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Inject
    LedgerService ledgerService;

    @Test
    public void doubleReversal() throws IOException {
        var loanId = postAll("double-reversal");
        checkImpacts(loanId, "double-reversal");
    }

    @Test
    public void backdatedPaymentStaleFork() throws IOException {
        var loanId = postAll("backdated-payment-stale-fork");
        checkImpacts(loanId, "backdated-payment-stale-fork");
        var ledger = ledgerService.getLedger(loanId);
        var sum = ledger.getEntries().stream().map(LedgerEntry::getBalanceChange).reduce(Balance::add).orElseThrow();
        assertTrue(sum.equals(ledger.getCurrentBalance()),
                "sum of entries " + sum + " differs from the current balance " + ledger.getCurrentBalance());
    }

    private UUID postAll(String scenario) throws IOException {
        var loanId = createLoan();
        var path = DATA_PATH + scenario + "/ledger-activities.csv";
        try (var reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(path)), StandardCharsets.UTF_8)) {
            for (CSVRecord row : CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
                    .parse(reader)) {
                given().contentType(ContentType.JSON).pathParam("loanId", loanId).body(toJson(row))
                        .post("/api/v1/loans/{loanId}/ledger-activities").then().statusCode(200);
            }
        }
        return loanId;
    }

    /** The same JSON shape as {@code LedgerActivityE2ETest.activityJson}: empty CSV cells are left out. */
    private static String toJson(CSVRecord row) throws JsonProcessingException {
        Map<String, Object> body = new LinkedHashMap<>();
        for (var field : new String[]{"activityId", "commonName", "activityType", "transactionStrategy", "direction",
                "spread", "reversalActivityType", "reversalActivityId"}) {
            if (!row.get(field).isBlank()) body.put(field, row.get(field));
        }
        body.put("amount", row.get("amount").isBlank() ? 0.0 : Double.parseDouble(row.get("amount")));
        body.put("principal", 0.0);
        body.put("interest", 0.0);
        body.put("fee", 0.0);
        body.put("excess", 0.0);
        body.put("effectiveAt", row.get("effectiveAt"));
        body.put("transactionTime", row.get("transactionTime"));
        return JSON.writeValueAsString(body);
    }

    private void checkImpacts(UUID loanId, String scenario) throws IOException {
        var ledger = ledgerService.getLedger(loanId);
        var expectations = new CSVUtil<LedgerActivityImpactExpectation>().parse(
                DATA_PATH + scenario + "/expectations.csv", LedgerActivityImpactExpectation.class);
        for (var expectation : expectations) {
            var actual = ledger.calculateTotalImpact(expectation.activityType(), expectation.activityId());
            assertTrue(expectation.getImpact().equals(actual), scenario + ": activity " + expectation.activityId()
                    + " expected impact " + expectation.getImpact() + " but was " + actual);
        }
    }

    private UUID createLoan() {
        var response = given().contentType(ContentType.JSON)
                .body(Map.of("daysInYear", 365, "currencyCode", "USD", "externalId", "",
                        "lastLedgerFreezePrincipalBalance", 0.0, "lastLedgerFreezeInterestBalance", 0.0,
                        "lastLedgerFreezeFeeBalance", 0.0, "lastLedgerFreezeExcessBalance", 0.0,
                        "lastLedgerFrozenOn", DB_SAFE_LOCAL_DATETIME_MIN.toString()))
                .post("/api/v1/loans");
        response.then().statusCode(200);
        return response.body().jsonPath().getUUID("id");
    }
}
