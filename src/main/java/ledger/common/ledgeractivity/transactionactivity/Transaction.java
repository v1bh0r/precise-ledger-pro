package ledger.common.ledgeractivity.transactionactivity;

import ledger.common.Ledger;
import ledger.common.LedgerActivity;
import ledger.common.TransactionSpreadStrategy;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityContext;
import ledger.model.GeneralLedgerActivity;
import ledger.model.LedgerClock;
import ledger.model.LedgerEntry;
import ledger.service.LedgerService;
import lombok.Getter;
import lombok.NonNull;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;

import static ledger.util.MonetaryUtil.toDouble;

@Getter
public class Transaction extends LedgerActivity {
    @NonNull
    MonetaryAmount amount;
    @NonNull
    TransactionSpreadStrategy transactionSpreadStrategy;
    @NonNull
    LedgerService ledgerService;

    public Transaction(@NonNull String loanId, @NonNull String commonName, @NonNull String activityType,
                       @NonNull String activityId, @NonNull MonetaryAmount amount,
                       @NonNull TransactionSpreadStrategy transactionSpreadStrategy,
                       @NonNull LocalDateTime effectiveAt, @NonNull LocalDateTime createdAt,
                       @NonNull LedgerService ledgerService) {
        super(loanId, commonName, activityType, activityId, effectiveAt, createdAt);
        this.amount = amount;
        this.transactionSpreadStrategy = transactionSpreadStrategy;
        this.ledgerService = ledgerService;
    }

    public Transaction(@NonNull GeneralLedgerActivity generalLedgerActivity, @NonNull LedgerService ledgerService) {
        super(generalLedgerActivity.getLoanId(), generalLedgerActivity.getCommonName(),
                generalLedgerActivity.getActivityType(), generalLedgerActivity.getActivityId(),
                generalLedgerActivity.getEffectiveAt(), generalLedgerActivity.getTransactionTime());
        this.amount = generalLedgerActivity.getAmount();
        this.transactionSpreadStrategy = TransactionSpreadStrategyFactory.create(generalLedgerActivity);
        this.ledgerService = ledgerService;
    }

    @Override
    public void generateLedgerEntries(Ledger ledger,
                                      LedgerClock ledgerClock, TemporalActivityContext temporalActivityContext) {
        var currentBalance = ledger.getCurrentBalance();
        var balance = transactionSpreadStrategy.applyTo(currentBalance);
        var change = balance.subtract(currentBalance);
        ledger.addEntry(LedgerEntry.builder()
                .entryType(super.getCommonName()).loanId(ledger.getLoanId()).amount(toDouble(this.amount))
                .createdAt(LocalDateTime.now()).effectiveAt(this.getEffectiveAt())
                .principal(toDouble(change.principal()))
                .interest(toDouble(change.interest())).fee(toDouble(change.fee())).excess(toDouble(change.excess()))
                .principalBalance(toDouble(balance.principal())).interestBalance(toDouble(balance.interest()))
                .feeBalance(toDouble(balance.fee()))
                .excessBalance(toDouble(balance.excess())).sourceLedgerActivityId(super.getActivityId())
                .sourceLedgerActivityType(super.getActivityType()).build());
    }
}
