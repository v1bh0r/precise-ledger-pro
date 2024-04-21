package ledger.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Setter
public abstract class LedgerActivity {
    @NonNull
    String loanId;
    @NonNull
    String commonName;
    @NonNull
    String activityType;
    @NonNull
    String activityId;
    @NonNull
    LocalDateTime effectiveAt;
    @NonNull
    LocalDateTime createdAt;

    /**
     * Adds one or more Ledger Activities to the ledger depending on the type of Ledger Activity
     *
     * @param ledger the ledger to which the Ledger Activities are added
     */
    public abstract void applyTo(Ledger ledger);
}
