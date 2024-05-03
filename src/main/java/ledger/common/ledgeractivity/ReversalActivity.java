package ledger.common.ledgeractivity;

import ledger.common.Ledger;
import ledger.common.LedgerActivity;
import ledger.model.GeneralLedgerActivity;
import ledger.model.LedgerClock;
import ledger.service.LedgerService;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class ReversalActivity extends LedgerActivity {
    @NonNull
    String reversedActivityType;
    @NonNull
    String reversedActivityId;
    @NonNull
    LedgerService ledgerService;

    public ReversalActivity(@NonNull GeneralLedgerActivity generalLedgerActivity,
                            @NonNull LedgerService ledgerService) {
        super(generalLedgerActivity.getLoanId(), generalLedgerActivity.getCommonName(),
                generalLedgerActivity.getActivityType(), generalLedgerActivity.getActivityId(),
                generalLedgerActivity.getEffectiveAt(), generalLedgerActivity.getCreatedAt());
        this.reversedActivityType = generalLedgerActivity.getReversalActivityType();
        this.reversedActivityId = generalLedgerActivity.getReversalActivityId();
        this.ledgerService = ledgerService;
    }

    @Override
    public void generateLedgerEntries(Ledger ledger,
                                      LedgerClock ledgerClock) {
        ledgerService.reverseLedgerActivity(this, ledger, ledgerClock);
    }
}
