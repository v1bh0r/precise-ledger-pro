package ledger.api.loan;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;

@QuarkusTest
public class LoanResourceTest {
    @Test
    public void testDeleteLoan() {

        // Assume a loan is created here for test purposes
        var createLoanResponse = given()
                .contentType("application/json")
                .body(new LoanCreationRequest(365, "USD", "asdf", 0.0, 0.0, 0.0, 0.0, DB_SAFE_LOCAL_DATETIME_MIN))
                .post("/api/v1/loans");

        createLoanResponse.then().statusCode(200);
        var loanId = createLoanResponse.body().jsonPath().getUUID("id");


        given()
                .pathParam("loanId", loanId)
                .get("/api/v1/loans/{loanId}")
                .then().statusCode(200);

        // Attempt to delete the loan
        given()
                .pathParam("loanId", loanId)
                .when().delete("/api/v1/loans/{loanId}")
                .then()
                .statusCode(200);  // Check if the status code is 200 OK
    }
}
