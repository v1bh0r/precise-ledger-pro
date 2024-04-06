package ledger.common.ledgeractivity.transactionactivity;

import ledger.common.Ledger;
import ledger.common.ledgeractivity.transactionactivity.transactionspreadstrategy.StaticSpread;
import ledger.model.Balance;
import ledger.model.Direction;
import ledger.model.LedgerEntry;
import lombok.NonNull;

import java.time.LocalDateTime;

public class StaticAllocationTransaction extends Transaction {
    @NonNull
    private final Balance customSpreadOverride;

    public StaticAllocationTransaction(@NonNull String loanId, @NonNull String activityType, @NonNull String activityId,
                                       @NonNull Balance customSpreadOverride, @NonNull Direction direction,
                                       @NonNull LocalDateTime effectiveAt, @NonNull LocalDateTime createdAt) {
        super(loanId, activityType, activityId, customSpreadOverride.getTotalAmount(),
                new StaticSpread(customSpreadOverride, direction), effectiveAt, createdAt);
        this.customSpreadOverride = customSpreadOverride;
    }

    @Override
    public void applyTo(Ledger ledger) {
        var currentBalance = ledger.getCurrentBalance();
        var newBalance = transactionSpreadStrategy.applyTo(ledger.getCurrentBalance());
        var difference = newBalance.subtract(currentBalance);
        ledger.addEntry(LedgerEntry.builder()
                .loanId(super.getLoanId())
                .entryType(super.getActivityType())
                .amount(customSpreadOverride.getTotalAmount())
                .principal(difference.principal())
                .interest(difference.interest())
                .fee(difference.fee())
                .excess(difference.excess())
                .principalBalance(newBalance.principal())
                .interestBalance(newBalance.interest())
                .feeBalance(newBalance.fee())
                .excessBalance(newBalance.excess())
                .effectiveAt(super.getEffectiveAt())
                .createdAt(super.getCreatedAt())
                .sourceLedgerActivityType(super.getActivityType())
                .sourceLedgerActivityId(super.getActivityId())
                .build());
    }
}
