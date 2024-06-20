package ledger.api.loan;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import ledger.common.ledgeractivity.domain.Loan;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("/api/v1/loans")
public class LoanResource {
    @Inject
    LoanMapper loanMapper;

    @Inject
    Logger logger;

    @GET
    @Path("/")
    public List<LoanResponse> getLoans() {
        List<Loan> loans = Loan.listAll();
        return loans.stream().map(loanMapper::toLoanResponse).toList();
    }

    @GET
    @Path("/{loanId}")
    public LoanResponse getLoan(@PathParam("loanId") UUID loanId) {
        logger.info("Getting loan with id: " + loanId);
        Optional<Loan> optional = Loan.findByIdOptional(loanId);
        var loan = optional.orElseThrow(NotFoundException::new);
        return loanMapper.toLoanResponse(loan);
    }

    @POST
    @Path("/")
    @Transactional
    public LoanResponse createLoan(LoanCreationRequest request) {
        var loan = loanMapper.toLoan(request);
        loan.persist();
        return loanMapper.toLoanResponse(loan);
    }

    @DELETE
    @Path("/{loanId}")
    @Transactional
    public LoanResponse deleteLoan(@PathParam("loanId") UUID loanId) {
        Optional<Loan> optional = Loan.findByIdOptional(loanId);
        var loan = optional.orElseThrow(NotFoundException::new);
        loan.delete();
        return loanMapper.toLoanResponse(loan);
    }
}
