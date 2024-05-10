package ledger.api.interestrate;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ledger.common.ledgeractivity.domain.InterestRate;

@Path("/api/v1/interest-rates")
@Produces(MediaType.APPLICATION_JSON)
public class InterestRateResource {


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response createInterestRate(InterestRate interestRate) {
        interestRate.persist();
        return Response.status(Response.Status.CREATED).entity(interestRate).build();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getInterestRates(@QueryParam("loanId") String loanId) {
        var interestRates = InterestRate.find("loanId", loanId).list();
        return Response.ok(interestRates).build();
    }
}
