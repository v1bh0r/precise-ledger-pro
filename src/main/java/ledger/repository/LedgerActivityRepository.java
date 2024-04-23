package ledger.repository;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.LedgerActivity;
import ledger.common.LedgerActivityFactory;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityContext;
import ledger.model.GeneralLedgerActivity;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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

    public List<LedgerActivity> findByLoanIdAndCreatedAfter(LedgerActivity ledgerActivity) {
        return ledgerActivities.stream()
                .filter(activity -> activity.getLoanId().equals(ledgerActivity.getLoanId())
                        && (activity.getCreatedAt().isAfter(ledgerActivity.getCreatedAt())
                        || (activity.getCreatedAt().isEqual(ledgerActivity.getCreatedAt())
                        && !activity.getActivityId().equals(ledgerActivity.getActivityId()))))
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

    public List<LedgerActivity> getLedgerActivitiesCreatedSince(String loanId, String ledgerActivityType,
                                                                String ledgerActivityId) {
        var la = this.findFirstByLoanIdAndTypeAndId(loanId, ledgerActivityType, ledgerActivityId);
        if (la == null) {
            return new ArrayList<>();
        }
        return findByLoanIdAndCreatedAfter(la);
    }
}
