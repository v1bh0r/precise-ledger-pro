package ledger.api.loan;

import ledger.common.ledgeractivity.domain.Loan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface LoanMapper {
    LoanResponse toLoanResponse(Loan loan);

    Loan toLoan(LoanCreationRequest request);
}
