package ledger.service;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.LedgerActivity;
import ledger.common.ledgeractivity.domain.InterestRate;
import ledger.repository.InterestRateRepository;
import ledger.repository.LedgerActivityRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class LoanService {
    @NonNull
    LedgerActivityRepository ledgerActivityRepository;

    @NonNull
    InterestRateRepository interestRateRepository;

    public List<InterestRate> getEffectiveInterestRates(String loanId) {
        return interestRateRepository.getEffectiveInterestRates(loanId);
    }

    public List<LedgerActivity> getLedgerActivitiesCreatedSince(String loanId, String ledgerActivityType,
                                                                String ledgerActivityId) {
        return ledgerActivityRepository.getLedgerActivitiesCreatedSince(loanId, ledgerActivityType, ledgerActivityId);
    }
}
