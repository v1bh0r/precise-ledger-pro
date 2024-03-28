package ledger.common.ledgeractivity;

import ledger.common.LedgerActivity;
import lombok.NonNull;

public abstract class TemporalActivity extends LedgerActivity {
    public TemporalActivity(@NonNull String loanId, @NonNull String activityType, @NonNull String activityId) {
        super(loanId, activityType, activityId);
    }
}
