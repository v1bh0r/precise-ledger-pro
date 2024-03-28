package ledger.common;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public abstract class LedgerActivity {
    @NonNull
    String loanId;
    @NonNull
    String activityType;
    @NonNull
    String activityId;

    public abstract void applyTo(Ledger ledger);
}
