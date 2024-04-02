package ledger.common.ledgeractivity.transactionactivity;

import ledger.common.Ledger;
import ledger.common.LedgerActivity;
import ledger.common.TransactionSpreadStrategy;
import lombok.Getter;
import lombok.NonNull;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;

@Getter
public class Transaction extends LedgerActivity {
    @NonNull
    MonetaryAmount amount;
    @NonNull TransactionSpreadStrategy transactionSpreadStrategy;

    public Transaction(@NonNull String loanId, @NonNull String activityType, @NonNull String activityId,
                       @NonNull MonetaryAmount amount, @NonNull TransactionSpreadStrategy transactionSpreadStrategy,
                       @NonNull LocalDateTime effectiveAt, @NonNull LocalDateTime createdAt) {
        super(loanId, activityType, activityId, effectiveAt, createdAt);
        this.amount = amount;
        this.transactionSpreadStrategy = transactionSpreadStrategy;
    }

    @Override
    public void applyTo(Ledger ledger) {

    }
}
