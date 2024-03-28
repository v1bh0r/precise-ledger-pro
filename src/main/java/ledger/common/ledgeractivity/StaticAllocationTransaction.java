package ledger.common.ledgeractivity;

import ledger.common.Ledger;
import ledger.common.LedgerActivity;
import ledger.model.Balance;
import ledger.model.Direction;
import lombok.NonNull;

public class StaticAllocationTransaction extends LedgerActivity {
    @NonNull
    private Balance customSpreadOverride;
    @NonNull
    private Direction direction;

    public StaticAllocationTransaction(@NonNull String loanId, @NonNull String activityType, @NonNull String activityId,
                                       @NonNull Balance customSpreadOverride, @NonNull Direction direction) {
        super(loanId, activityType, activityId);
        this.customSpreadOverride = customSpreadOverride;
        this.direction = direction;
    }

    @Override
    public void applyTo(Ledger ledger) {

    }
}
