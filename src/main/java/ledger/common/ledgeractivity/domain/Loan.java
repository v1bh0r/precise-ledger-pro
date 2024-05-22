package ledger.common.ledgeractivity.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import ledger.model.Balance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

import static ledger.config.AppConfig.DEFAULT_CURRENCY_CODE;
import static ledger.config.AppConfig.DEFAULT_DAYS_IN_YEAR;
import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Builder
public class Loan extends PanacheEntityBase {
    @Id
    @UuidGenerator
    UUID id;
    @Builder.Default
    Integer daysInYear = DEFAULT_DAYS_IN_YEAR;
    @Builder.Default
    String currencyCode = DEFAULT_CURRENCY_CODE;
    String externalId;
    @Builder.Default
    Double lastLedgerFreezePrincipalBalance = 0.0;
    @Builder.Default
    Double lastLedgerFreezeInterestBalance = 0.0;
    @Builder.Default
    Double lastLedgerFreezeFeeBalance = 0.0;
    @Builder.Default
    Double lastLedgerFreezeExcessBalance = 0.0;
    @Builder.Default
    LocalDateTime lastLedgerFrozenOn = DB_SAFE_LOCAL_DATETIME_MIN;

    public Loan() {
        super();
    }

    public Balance getStartingLedgerBalance() {
        return new Balance(lastLedgerFreezePrincipalBalance, lastLedgerFreezeInterestBalance,
                lastLedgerFreezeFeeBalance, lastLedgerFreezeExcessBalance);
    }
    // TODO: We don't allow past-dated activity after a certain period of time. We need to figure out the best way to
    //  perform a ledger freeze and update the above fields along with archival of
    //  ledger entries
}
