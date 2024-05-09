package ledger.api.loan;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import ledger.common.ledgeractivity.domain.Loan;

import java.util.Optional;
import java.util.UUID;

@Path("/api/v1/loans")
@Produces(MediaType.APPLICATION_JSON)
public class LoanResource {
    @Inject
    LoanMapper loanMapper;

    @GET
    @Path("/{id}")
    public LoanResponse getLoan(UUID id) {
        Optional<Loan> optional = Loan.findByIdOptional(id);
        var loan = optional.orElseThrow(NotFoundException::new);
        return loanMapper.toLoanResponse(optional.get());
    }
    
    @POST
    @Transactional
    public LoanResponse createLoan(LoanCreationRequest request) {
        var loan = loanMapper.toLoan(request);
        loan.persist();
        return loanMapper.toLoanResponse(loan);
    }
}
