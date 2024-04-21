package ledger.repository;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.LedgerActivity;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class LedgerActivityRepository {
    private final List<LedgerActivity> ledgerActivities = new ArrayList<>();

    public LedgerActivity findByTypeAndId(String type, String id) {
        return ledgerActivities.stream()
                .filter(activity -> activity.getActivityType().equals(type) && activity.getActivityId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public LedgerActivity insert(LedgerActivity ledgerActivity) {
        ledgerActivities.add(ledgerActivity);
        return ledgerActivity;
    }
}
