package ledger.api.loan;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import ledger.common.ledgeractivity.domain.Loan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("/")
public class LoanResource {
    @Inject
    LoanMapper loanMapper;

    @GET
    @Path("/api/v1/loans")
    public List<LoanResponse> getLoans() {
        List<Loan> loans = Loan.listAll();
        return loans.stream().map(loanMapper::toLoanResponse).toList();
    }

    @GET
    @Path("/api/v1/loans/{loanId}")
    public LoanResponse getLoan(@PathParam("loanId") UUID loanId) {
        Optional<Loan> optional = Loan.findByIdOptional(loanId);
        var loan = optional.orElseThrow(NotFoundException::new);
        return loanMapper.toLoanResponse(optional.get());
    }

    @POST
    @Path("/api/v1/loans")
    @Transactional
    public LoanResponse createLoan(LoanCreationRequest request) {
        var loan = loanMapper.toLoan(request);
        loan.persist();
        return loanMapper.toLoanResponse(loan);
    }

    @DELETE
    @Path("/api/v1/loans/{loanId}")
    @Transactional
    public LoanResponse deleteLoan(@PathParam("loanId") UUID loanId) {
        Optional<Loan> optional = Loan.findByIdOptional(loanId);
        optional.ifPresent(Loan::delete);
        return optional.map(loan -> loanMapper.toLoanResponse(loan)).orElse(null);
    }
}
