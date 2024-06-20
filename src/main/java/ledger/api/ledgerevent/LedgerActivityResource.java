package ledger.api.ledgerevent;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import ledger.common.LedgerActivityFactory;
import ledger.model.GeneralLedgerActivity;
import ledger.service.LedgerService;
import ledger.service.LoanService;

import java.util.List;
import java.util.UUID;

import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;

@Path("/api/v1/loans/{loanId}/ledger-activities")
@Produces("application/json")
public class LedgerActivityResource {
    @Inject
    LoanService loanService;
    @Inject
    LedgerActivityFactory ledgerActivityFactory;
    @Inject
    LedgerService ledgerService;

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public GeneralLedgerActivity reportLedgerActivity(@PathParam("loanId") UUID loanId,
                                                      GeneralLedgerActivity generalLedgerActivity) {
        generalLedgerActivity.setLoanId(loanId.toString());
        generalLedgerActivity.persist();
        var temporalContext = loanService.getTemporalActivityContext(loanId);
        var ledgerActivity = ledgerActivityFactory.create(generalLedgerActivity);
        var ledger = ledgerService.getLedger(loanId, DB_SAFE_LOCAL_DATETIME_MIN);
        ledgerService.applyLedgerActivity(ledger, ledgerActivity, ledgerService.getCurrentLedgerClock(ledger),
                temporalContext);
        ledger.getEntries().forEach(ledgerEntry -> ledgerEntry.persist());
        return generalLedgerActivity;
    }

    @GET
    public List<GeneralLedgerActivity> getLedgerActivity(@PathParam("loanId") UUID loanId) {
        return GeneralLedgerActivity.list("loanId", loanId.toString());
    }
}
