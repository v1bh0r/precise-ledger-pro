package ledger.common.ledgeractivity.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
public class Loan extends PanacheEntityBase {
    @Id
    String id;
    @Getter
    Integer daysInYear;
    @Getter
    String currencyCode;
}
