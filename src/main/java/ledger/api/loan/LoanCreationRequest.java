package ledger.api.loan;

import lombok.Data;

import java.time.LocalDateTime;

import static ledger.config.AppConfig.DEFAULT_CURRENCY_CODE;
import static ledger.config.AppConfig.DEFAULT_DAYS_IN_YEAR;
import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;

@Data
public class LoanCreationRequest {
    private Integer daysInYear = DEFAULT_DAYS_IN_YEAR;
    private String currencyCode = DEFAULT_CURRENCY_CODE;
    private String externalId;
    private Double lastLedgerFreezePrincipalBalance = 0.0;
    private Double lastLedgerFreezeInterestBalance = 0.0;
    private Double lastLedgerFreezeFeeBalance = 0.0;
    private Double lastLedgerFreezeExcessBalance = 0.0;
    private LocalDateTime lastLedgerFrozenOn = DB_SAFE_LOCAL_DATETIME_MIN;
}
