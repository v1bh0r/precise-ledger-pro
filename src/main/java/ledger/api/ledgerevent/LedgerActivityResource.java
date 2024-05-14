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

import java.time.LocalDateTime;
import java.util.UUID;

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
    public GeneralLedgerActivity reportLedgerActivity(@PathParam("loanId") UUID loanId,
                                                      GeneralLedgerActivity generalLedgerActivity) {
        generalLedgerActivity.setLoanId(loanId.toString());
        generalLedgerActivity.persist();
        var temporalContext = loanService.getTemporalActivityContext(loanId);
        var ledgerActivity = ledgerActivityFactory.create(generalLedgerActivity);
        var ledger = ledgerService.getLedger(loanId, LocalDateTime.MIN);
        ledgerService.applyLedgerActivity(ledger, ledgerActivity, ledgerService.getCurrentLedgerClock(ledger),
                temporalContext);
        ledger.getEntries().forEach(ledgerEntry -> ledgerEntry.persist());
        return generalLedgerActivity;
    }
}
