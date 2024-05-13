package ledger.api.ledgerevent;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import ledger.common.LedgerActivityFactory;
import ledger.model.GeneralLedgerActivity;
import ledger.service.LedgerService;
import ledger.service.LoanService;

import java.util.UUID;

@Path("/")
@Produces("application/json")
public class LedgerActivityResource {
    @Inject
    LoanService loanService;
    @Inject
    LedgerActivityFactory ledgerActivityFactory;
    @Inject
    LedgerService ledgerService;

    @POST
    @Path("/api/v1/loans/{loanId}/ledger-activities")
    @Transactional
    public GeneralLedgerActivity reportLedgerActivity(@PathParam("loanId") UUID loanId,
                                                      GeneralLedgerActivity generalLedgerActivity) {
        generalLedgerActivity.persist();
        var temporalContext = loanService.getTemporalActivityContext(loanId);
        var ledgerActivity = ledgerActivityFactory.create(generalLedgerActivity);
        var ledger = ledgerService.getLedger(loanId);
        ledgerService.applyLedgerActivity(ledger, ledgerActivity, ledgerService.getCurrentLedgerClock(ledger),
                temporalContext);
        return generalLedgerActivity;
    }
}
