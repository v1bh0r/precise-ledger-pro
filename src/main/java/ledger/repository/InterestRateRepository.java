package ledger.repository;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.ledgeractivity.domain.InterestRate;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class InterestRateRepository {
    // TODO: Store in a database
    private final List<InterestRate> interestRates = new ArrayList<>();

    private void addInterestRate(InterestRate interestRate) {
        interestRates.add(interestRate);
    }

    public List<InterestRate> getEffectiveInterestRates(String loanId) {
        return interestRates.stream().filter(rate -> rate.getLoanId().equals(loanId))
                .sorted((r1, r2) -> r2.getEffectiveAt().compareTo(r1.getEffectiveAt())).toList();
    }
}
