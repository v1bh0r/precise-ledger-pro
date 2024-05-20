package ledger.api.loan;

import lombok.Data;

import java.time.LocalDateTime;

import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;

@Data
public class LoanCreationRequest {
    private Integer daysInYear;
    private String currencyCode;
    private String externalId;
    private LocalDateTime lastLedgerFrozenOn = DB_SAFE_LOCAL_DATETIME_MIN;
}
