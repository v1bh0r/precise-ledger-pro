package ledger.api.ledger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import ledger.model.LedgerEntry;
import ledger.service.LedgerService;
import ledger.util.ObjectToCsvUtil;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/loans")
public class LedgerResource {
    @Inject
    LedgerService ledgerService;

    ObjectToCsvUtil<LedgerEntry> objectToCsvUtil = new ObjectToCsvUtil<>();

    @GET
    @Path("/{loanId}/ledger")
    public List<LedgerEntry> getLedger(@PathParam("loanId") UUID loanId) {
        var ledger = ledgerService.getLedger(loanId);
        return ledger.getEntries();
    }

    @GET
    @Path("/{loanId}/ledger.csv")
    public Response getLedgerCsv(@PathParam("loanId") UUID loanId) throws IllegalAccessException {
        var ledger = ledgerService.getLedger(loanId);
        return Response.ok(objectToCsvUtil.generateCSV(ledger.getEntries())).type("text/csv").build();
    }
}
