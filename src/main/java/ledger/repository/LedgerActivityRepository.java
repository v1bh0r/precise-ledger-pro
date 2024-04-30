package ledger.repository;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.LedgerActivity;
import ledger.common.LedgerActivityFactory;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityContext;
import ledger.model.GeneralLedgerActivity;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class LedgerActivityRepository {
    @NonNull
    private LedgerActivityFactory ledgerActivityFactory;
    // TODO: Store the ledger activities in a database
    @Getter
    private final List<LedgerActivity> ledgerActivities = new ArrayList<>();

    public LedgerActivity findFirstByLoanIdAndTypeAndId(String loanId, String type, String id) {
        return ledgerActivities.stream()
                .filter(activity -> activity.getLoanId().equals(loanId) && activity.getActivityType()
                        .equals(type) && activity.getActivityId().equals(id)).findFirst().orElse(null);
    }

    public List<LedgerActivity> findByLoanIdAndCreatedAfterButBefore(LedgerActivity ledgerActivity,
                                                                     LocalDateTime createdAt) {
        return ledgerActivities.stream()
                .filter(activity -> (activity.getLoanId().equals(ledgerActivity.getLoanId())
                        && (activity.getCreatedAt().isAfter(ledgerActivity.getCreatedAt())
                        || (activity.getCreatedAt().isEqual(ledgerActivity.getCreatedAt())
                        && !activity.getActivityId()
                        .equals(ledgerActivity.getActivityId())))) && activity.getCreatedAt().isBefore(createdAt))
                .toList();
    }

    public LedgerActivity insert(LedgerActivity ledgerActivity) {
        ledgerActivities.add(ledgerActivity);
        return ledgerActivity;
    }

    public LedgerActivity insert(GeneralLedgerActivity generalLedgerActivity,
                                 TemporalActivityContext temporalActivityContext) {
        var ledgerActivity = ledgerActivityFactory.create(generalLedgerActivity, temporalActivityContext);
        return insert(ledgerActivity);
    }

    public List<LedgerActivity> getLedgerActivitiesCreatedSinceButBeforeCreatedAt(String loanId,
                                                                                  String ledgerActivityType,
                                                                                  String ledgerActivityId,
                                                                                  LocalDateTime createdAt) {
        var la = this.findFirstByLoanIdAndTypeAndId(loanId, ledgerActivityType, ledgerActivityId);
        if (la == null) {
            return new ArrayList<>();
        }
        return findByLoanIdAndCreatedAfterButBefore(la, createdAt);
    }

    public List<LedgerActivity> getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBefore(String loanId,
                                                                                          LocalDateTime effectiveAt,
                                                                                          LocalDateTime createdAt) {
        return ledgerActivities.stream()
                .filter(activity -> activity.getLoanId().equals(loanId) && (activity.getEffectiveAt()
                        .isAfter(effectiveAt)) || activity.getEffectiveAt()
                        .isEqual(effectiveAt) && (activity.getCreatedAt().isBefore(createdAt)))
                .toList();
    }

    public void flush() {
        ledgerActivities.clear();
    }
}
