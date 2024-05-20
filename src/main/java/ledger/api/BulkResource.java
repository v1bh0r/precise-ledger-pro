package ledger.api;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import ledger.common.LedgerActivityFactory;
import ledger.common.ledgeractivity.domain.InterestRate;
import ledger.model.GeneralLedgerActivity;
import ledger.service.LedgerService;
import ledger.service.LoanService;
import ledger.util.CSVUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.UUID;

import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;

@Path("/bulk/api/v1")
@Produces("application/json")
public class BulkResource {
    private final CSVUtil<InterestRate> interestRateCSVUtil = new CSVUtil<>();
    private final CSVUtil<GeneralLedgerActivity> generalLedgerActivityCSVUtil = new CSVUtil<>();
    @Inject
    LoanService loanService;
    @Inject
    LedgerActivityFactory ledgerActivityFactory;
    @Inject
    LedgerService ledgerService;

    // Create a multipart form data endpoint that accepts a CSV file and persists the interest rates
    @POST
    @Path("/loans/{loanId}/interest-rates")
    @Consumes("text/csv")
    @Transactional
    public Response createInterestRatesBulk(@PathParam("loanId") String loanId, InputStream fileInputStream) {
        var reader = new BufferedReader(new InputStreamReader(fileInputStream));
        var interestRates = interestRateCSVUtil.parse(reader, InterestRate.class);

        interestRates.forEach(interestRate -> {
            interestRate.setLoanId(loanId);
            interestRate.persist();
        });

        return Response.status(Response.Status.CREATED).entity(interestRates).build();
    }

    @POST
    @Path("/loans/{loanId}/ledger-activities")
    @Consumes("text/csv")
    @Transactional
    public Response reportLedgerActivityBulk(@PathParam("loanId") UUID loanId, InputStream fileInputStream) {
        var reader = new BufferedReader(new InputStreamReader(fileInputStream));
        var generalLedgerActivities = generalLedgerActivityCSVUtil.parse(reader, GeneralLedgerActivity.class);

        var temporalContext = loanService.getTemporalActivityContext(loanId);

        var ledger = ledgerService.getLedger(loanId, DB_SAFE_LOCAL_DATETIME_MIN);

        generalLedgerActivities.stream().sorted(Comparator.comparing(GeneralLedgerActivity::getTransactionTime))
                .forEach(generalLedgerActivity -> {
                    generalLedgerActivity.setLoanId(loanId.toString());
                    generalLedgerActivity.persist();
                    var ledgerActivity = ledgerActivityFactory.create(generalLedgerActivity);
                    ledgerService.applyLedgerActivity(ledger, ledgerActivity,
                            ledgerService.getCurrentLedgerClock(ledger), temporalContext);
                });

        ledger.getEntries().forEach(ledgerEntry -> ledgerEntry.persist());

        return Response.status(Response.Status.CREATED).entity(generalLedgerActivities).build();
    }
}
