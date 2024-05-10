package ledger.api.ledgerevent;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import ledger.common.LedgerActivityFactory;
import ledger.model.GeneralLedgerActivity;

@Path("/")
@Produces("application/json")
public class LedgerActivityResource {
    @Inject
    LedgerActivityFactory ledgerActivityFactory;

    @POST
    @Path("/api/v1/loans/{loanId}/ledger-activities")
    public GeneralLedgerActivity reportLedgerActivity(GeneralLedgerActivity ledgerActivity) {
        ledgerActivity.persist();
        return ledgerActivity;
    }
}
