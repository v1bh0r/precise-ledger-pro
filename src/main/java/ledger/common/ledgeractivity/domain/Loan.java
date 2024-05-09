package ledger.common.ledgeractivity.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;


@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Loan extends PanacheEntityBase {
    @Id
    @UuidGenerator
    UUID id;
    Integer daysInYear;
    String currencyCode = "USD";
    String externalId;
}
