package ledger.model;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;

public record LedgerEntry(String loanId, String eventId, String eventType, MonetaryAmount amount, MonetaryAmount principal,
                          MonetaryAmount interest, MonetaryAmount excess, MonetaryAmount fee,
                          MonetaryAmount principalBalance, MonetaryAmount interestBalance, MonetaryAmount feeBalance,
                          MonetaryAmount excessBalance, LocalDateTime effectiveAt, LocalDateTime createdAt,
                          String sourceLedgerActivityType, String sourceLedgerActivityId) {
}
