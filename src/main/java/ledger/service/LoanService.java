package ledger.service;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.LedgerActivity;
import ledger.common.ledgeractivity.domain.InterestRate;
import ledger.repository.InterestRateRepository;
import ledger.repository.LedgerActivityRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
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

    public List<LedgerActivity> getLedgerActivitiesCreatedSinceButBeforeCreatedAt(String loanId,
                                                                                  String ledgerActivityType,
                                                                                  String ledgerActivityId,
                                                                                  LocalDateTime createdAt) {
        return ledgerActivityRepository.getLedgerActivitiesCreatedSinceButBeforeCreatedAt(loanId, ledgerActivityType,
                ledgerActivityId, createdAt);
    }

    public List<LedgerActivity> getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBefore(String loanId,
                                                                                          LocalDateTime effectiveAt,
                                                                                          LocalDateTime createdAt) {
        return ledgerActivityRepository.getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBefore(loanId, effectiveAt
                , createdAt);
    }
}
