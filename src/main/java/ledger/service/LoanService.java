package ledger.service;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.ledgeractivity.domain.InterestRate;
import ledger.common.ledgeractivity.domain.Loan;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityContext;
import ledger.model.GeneralLedgerActivity;
import ledger.repository.LedgerActivityRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class LoanService {
    @NonNull
    LedgerActivityRepository ledgerActivityRepository;

    public List<InterestRate> getEffectiveInterestRates(UUID loanId) {
        return InterestRate.find("loanId", loanId).list();
    }

    public List<GeneralLedgerActivity> getLedgerActivitiesCreatedSinceButBeforeCreatedAt(String loanId,
                                                                                         String ledgerActivityType,
                                                                                         String ledgerActivityId,
                                                                                         LocalDateTime createdAt) {
        return ledgerActivityRepository.getLedgerActivitiesCreatedSinceButBeforeCreatedAt(loanId, ledgerActivityType,
                ledgerActivityId, createdAt);
    }

    public List<GeneralLedgerActivity> getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBefore(String loanId,
                                                                                                 LocalDateTime effectiveAt,
                                                                                                 LocalDateTime createdAt) {
        return ledgerActivityRepository.getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBefore(loanId, effectiveAt
                , createdAt);
    }

    public TemporalActivityContext getTemporalActivityContext(UUID loanId) {
        Loan loan = Loan.findById(loanId);
        var temporalContext = new TemporalActivityContext();

        temporalContext.setProperty("interestRates", this.getEffectiveInterestRates(loanId));
        temporalContext.setProperty("daysInYear", loan.getDaysInYear());
        temporalContext.setProperty("currencyCode", loan.getCurrencyCode());

        return temporalContext;
    }
}
