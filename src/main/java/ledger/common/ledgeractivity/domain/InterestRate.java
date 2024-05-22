package ledger.common.ledgeractivity.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.csv.CSVRecord;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Builder
public class InterestRate extends PanacheEntityBase {
    @Id
    @UuidGenerator
    UUID id;
    String loanId;
    Float rate;
    LocalDateTime effectiveAt;

    public InterestRate() {
        super();
    }

    @SuppressWarnings("unused")
    public InterestRate(CSVRecord record) {
        this.loanId = record.get("loanId");
        this.rate = Float.parseFloat(record.get("rate"));
        this.effectiveAt = LocalDateTime.parse(record.get("effectiveAt"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T" +
                "'HH:mm:ss"));
    }
}
