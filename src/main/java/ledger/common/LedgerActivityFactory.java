package ledger.common;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.ledgeractivity.ReversalActivity;
import ledger.common.ledgeractivity.temporalactivity.StartOfDay;
import ledger.common.ledgeractivity.transactionactivity.Transaction;
import ledger.model.GeneralLedgerActivity;
import ledger.service.LedgerService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;

@ApplicationScoped
@RequiredArgsConstructor
public class LedgerActivityFactory {
    @NonNull
    private LedgerService ledgerService;

    @NonNull
    Logger log;

    public LedgerActivity create(@NonNull GeneralLedgerActivity generalActivity) {
        //I need to parse the activity type
        return switch (generalActivity.getActivityType()) {
            case "Transaction" -> new Transaction(generalActivity, ledgerService);
            case "StartOfDay" -> new StartOfDay(generalActivity);
            case "Reversal" -> new ReversalActivity(generalActivity, ledgerService);
            default -> throw new RuntimeException("Unknown activity type: " + generalActivity.getActivityType());
        };
    }
}
