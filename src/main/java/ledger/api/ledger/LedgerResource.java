package ledger.api.ledger;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import ledger.model.LedgerEntry;
import ledger.service.LedgerService;
import ledger.util.ObjectToCsvUtil;

import java.util.UUID;

@Path("/api/v1/loans/{loanId}/ledger")
@Produces("application/json")
public class LedgerResource {
    @Inject
    LedgerService ledgerService;

    ObjectToCsvUtil<LedgerEntry> objectToCsvUtil = new ObjectToCsvUtil<>();

    @GET
    public Response getLedger(@HeaderParam("Accept") String acceptHeader, @PathParam("loanId") UUID loanId) throws IllegalAccessException {
        var ledger = ledgerService.getLedger(loanId);
        if (acceptHeader.equals("text/csv")) {
            return Response.ok(objectToCsvUtil.generateCSV(ledger.getEntries())).build();
        } else {
            return Response.ok(ledger).build();
        }
    }
}
