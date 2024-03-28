package ledger.model;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;

public record LedgerEntry(String loanId, String eventId, String eventType, MonetaryAmount amount,
                          MonetaryAmount principal,
                          MonetaryAmount interest, MonetaryAmount fee, MonetaryAmount excess,
                          MonetaryAmount principalBalance, MonetaryAmount interestBalance, MonetaryAmount feeBalance,
                          MonetaryAmount excessBalance, LocalDateTime effectiveAt, LocalDateTime createdAt,
                          String sourceLedgerActivityType, String sourceLedgerActivityId) {
    public Balance getBalance() {
        return new Balance(principalBalance, interestBalance, feeBalance, excessBalance);
    }
}
