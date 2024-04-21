package ledger.common.ledgeractivity;

import ledger.common.LedgerActivity;
import lombok.NonNull;

import java.time.LocalDateTime;

public abstract class TemporalActivity extends LedgerActivity {
    public TemporalActivity(@NonNull String loanId, @NonNull String commonName, @NonNull String activityType, @NonNull String activityId, @NonNull LocalDateTime effectiveAt, @NonNull LocalDateTime createdAt) {
        super(loanId, commonName, activityType, activityId, effectiveAt, createdAt);
    }
}
