package ledger.repository;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.model.GeneralLedgerActivity;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class LedgerActivityRepository {

    public GeneralLedgerActivity findFirstByLoanIdAndTypeAndId(String loanId, String type, String id) {
        return GeneralLedgerActivity.find("loanId = ?1 AND activityType = ?2 AND " +
                        "activityId = ?3", loanId, type, id)
                .firstResult();
    }

    public List<GeneralLedgerActivity> findByLoanIdAndCreatedAfterButBeforeOrEqual(GeneralLedgerActivity ledgerActivity,
                                                                                   LocalDateTime createdAt) {
        return GeneralLedgerActivity.find("loanId = ?1 AND transactionTime > ?2 AND transactionTime <= ?3",
                ledgerActivity.getLoanId(), ledgerActivity.getTransactionTime(), createdAt).list();
    }

    public GeneralLedgerActivity insert(GeneralLedgerActivity ledgerActivity) {
        ledgerActivity.persist();
        return ledgerActivity;
    }

    public List<GeneralLedgerActivity> getLedgerActivitiesCreatedSinceButBeforeCreatedAt(String loanId,
                                                                                         String ledgerActivityType,
                                                                                         String ledgerActivityId,
                                                                                         LocalDateTime createdAt) {
        var la = this.findFirstByLoanIdAndTypeAndId(loanId, ledgerActivityType, ledgerActivityId);
        if (la == null) {
            return new ArrayList<>();
        }
        return findByLoanIdAndCreatedAfterButBeforeOrEqual(la, createdAt);
    }

    public List<GeneralLedgerActivity> getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBefore(String loanId,
                                                                                                 LocalDateTime effectiveAt,
                                                                                                 LocalDateTime createdAt) {
        return GeneralLedgerActivity.find("loanId = ?1 AND effectiveAt >= ?2 AND transactionTime < ?3",
                loanId, effectiveAt, createdAt).list();
    }


    public void deleteAll() {
        GeneralLedgerActivity.deleteAll();
    }
}
