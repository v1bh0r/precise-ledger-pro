package ledger.api;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import ledger.common.ledgeractivity.domain.InterestRate;
import ledger.util.CSVUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@Path("/bulk/api/v1")
public class BulkResource {
    private final CSVUtil<InterestRate> interestRateCSVUtil = new CSVUtil<>();

    // Create a multipart form data endpoint that accepts a CSV file and persists the interest rates
    @POST
    @Path("/loans/{loanId}/interest-rates")
    @Consumes("text/csv")
    @Transactional
    public Response createInterestRatesBulk(String loanId, InputStream fileInputStream) {
        var reader = new BufferedReader(new InputStreamReader(fileInputStream));
        var interestRates = interestRateCSVUtil.parse(reader, InterestRate.class);

        interestRates.forEach(interestRate -> {
            interestRate.setLoanId(loanId);
            interestRate.persist();
        });

        return Response.status(Response.Status.CREATED).entity(interestRates).build();
    }
}
