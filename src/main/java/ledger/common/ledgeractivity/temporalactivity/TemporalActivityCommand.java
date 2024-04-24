package ledger.common.ledgeractivity.temporalactivity;

import ledger.model.Balance;
import ledger.model.LedgerEntry;

import java.time.LocalDateTime;

public interface TemporalActivityCommand {
    default String getName() {
        return this.getClass().getSimpleName();
    }

    LedgerEntry execute(String nextLedgerEntryId, String loanId, Balance loanBalance, String activityType, String activityId, LocalDateTime effectiveAt,
                        TemporalActivityContext context);
}
