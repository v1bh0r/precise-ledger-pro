package ledger.api.loan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

import static ledger.config.AppConfig.DEFAULT_CURRENCY_CODE;
import static ledger.config.AppConfig.DEFAULT_DAYS_IN_YEAR;
import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;

@Builder
@Getter
@AllArgsConstructor
public class LoanCreationRequest {
    @Setter
    private Integer daysInYear = DEFAULT_DAYS_IN_YEAR;
    @Setter
    private String currencyCode = DEFAULT_CURRENCY_CODE;
    @Setter
    private String externalId;

    private Double lastLedgerFreezePrincipalBalance = 0.0;

    @SuppressWarnings("unused")
    public void setLastLedgerFreezePrincipalBalance(Double lastLedgerFreezePrincipalBalance) {
        this.lastLedgerFreezePrincipalBalance = Objects.requireNonNullElse(lastLedgerFreezePrincipalBalance, 0.0);
    }

    private Double lastLedgerFreezeInterestBalance = 0.0;

    @SuppressWarnings("unused")
    public void setLastLedgerFreezeInterestBalance(Double lastLedgerFreezeInterestBalance) {
        this.lastLedgerFreezeInterestBalance = Objects.requireNonNullElse(lastLedgerFreezeInterestBalance, 0.0);
    }

    private Double lastLedgerFreezeFeeBalance = 0.0;

    @SuppressWarnings("unused")
    public void setLastLedgerFreezeFeeBalance(Double lastLedgerFreezeFeeBalance) {
        this.lastLedgerFreezeFeeBalance = Objects.requireNonNullElse(lastLedgerFreezeFeeBalance, 0.0);
    }

    private Double lastLedgerFreezeExcessBalance = 0.0;

    @SuppressWarnings("unused")
    public void setLastLedgerFreezeExcessBalance(Double lastLedgerFreezeExcessBalance) {
        this.lastLedgerFreezeExcessBalance = Objects.requireNonNullElse(lastLedgerFreezeExcessBalance, 0.0);
    }

    private LocalDateTime lastLedgerFrozenOn = DB_SAFE_LOCAL_DATETIME_MIN;

    @SuppressWarnings("unused")
    public void setLastLedgerFrozenOn(LocalDateTime lastLedgerFrozenOn) {
        this.lastLedgerFrozenOn = Objects.requireNonNullElse(lastLedgerFrozenOn, DB_SAFE_LOCAL_DATETIME_MIN);
    }
}
