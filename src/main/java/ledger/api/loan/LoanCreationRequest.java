package ledger.api.loan;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoanCreationRequest {
    private Integer daysInYear;
    private String currencyCode;
    private String externalId;
    private LocalDateTime lastLedgerFrozenOn = LocalDateTime.MIN;
}
