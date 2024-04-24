package ledger.common.ledgeractivity.domain;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class InterestRate extends PanacheEntityBase {
    @Id
    @GeneratedValue
    String id;

    @CsvBindByName
    String loanId;

    @CsvBindByName
    Float rate;

    @CsvBindByName
    @CsvDate(value = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime effectiveAt;
}
