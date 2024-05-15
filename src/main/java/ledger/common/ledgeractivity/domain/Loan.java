package ledger.common.ledgeractivity.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import ledger.model.Balance;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

import static ledger.common.MonetaryUtil.DEFAULT_CURRENCY_CODE;


@Entity
@Getter
@Setter
public class Loan extends PanacheEntityBase {
    @Id
    @UuidGenerator
    UUID id;
    Integer daysInYear;
    String currencyCode = DEFAULT_CURRENCY_CODE;
    String externalId;
    Double lastLedgerFreezePrincipalBalance = 0.0;
    Double lastLedgerFreezeInterestBalance = 0.0;
    Double lastLedgerFreezeFeeBalance = 0.0;
    Double lastLedgerFreezeExcessBalance = 0.0;
    LocalDateTime lastLedgerFrozenOn = LocalDateTime.MIN;

    public Balance getStartingLedgerBalance() {
        return new Balance(lastLedgerFreezePrincipalBalance, lastLedgerFreezeInterestBalance,
                lastLedgerFreezeFeeBalance, lastLedgerFreezeExcessBalance);
    }
    // TODO: We don't allow past-dated activity after a certain period of time. We need to figure out the best way to
    //  perform a ledger freeze and update the above fields along with archival of
    //  ledger entries
}
