package ledger.common.ledgeractivity.temporalactivity;

import ledger.common.Ledger;
import ledger.common.ledgeractivity.TemporalActivity;
import lombok.NonNull;

public class SOD extends TemporalActivity {
    public SOD(@NonNull String loanId, @NonNull String activityType, @NonNull String activityId) {
        super(loanId, activityType, activityId);
    }

    @Override
    public void applyTo(Ledger ledger) {

    }
}
