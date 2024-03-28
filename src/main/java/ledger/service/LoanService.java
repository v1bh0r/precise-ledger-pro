package ledger.service;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.ledgeractivity.domain.InterestRate;

import java.util.List;

@ApplicationScoped
public class LoanService {
    public List<InterestRate> getEffectiveInterestRates(String loanId) {
        return InterestRate.find("loanId = ?1 order by effectiveAt desc", loanId).list();
    }
}
