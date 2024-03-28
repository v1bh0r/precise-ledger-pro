package ledger.common.ledgeractivity;

import ledger.common.Ledger;
import ledger.common.LedgerActivity;
import ledger.common.TransactionSpreadStrategy;
import lombok.NonNull;

import javax.money.MonetaryAmount;

public class Transaction extends LedgerActivity {
    @NonNull
    MonetaryAmount amount;
    @NonNull TransactionSpreadStrategy transactionSpreadStrategy;

    public Transaction(@NonNull String loanId, @NonNull String activityType, @NonNull String activityId,
                       @NonNull MonetaryAmount amount, @NonNull TransactionSpreadStrategy transactionSpreadStrategy) {
        super(loanId, activityType, activityId);
        this.amount = amount;
        this.transactionSpreadStrategy = transactionSpreadStrategy;
    }

    @Override
    public void applyTo(Ledger ledger) {

    }
}
