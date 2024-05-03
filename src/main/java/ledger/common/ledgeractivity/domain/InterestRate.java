package ledger.common.ledgeractivity.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class InterestRate extends PanacheEntityBase {
    @Id
    @GeneratedValue
    String id;
    String loanId;
    Float rate;
    LocalDateTime effectiveAt;

    @SuppressWarnings("unused")
    public InterestRate(CSVRecord record) {
        this.loanId = record.get("loanId");
        this.rate = Float.parseFloat(record.get("rate"));
        this.effectiveAt = LocalDateTime.parse(record.get("effectiveAt"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T" +
                "'HH:mm:ss"));
    }
}
