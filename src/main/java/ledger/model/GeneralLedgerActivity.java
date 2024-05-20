package ledger.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import ledger.common.LedgerActivity;
import ledger.common.deserializer.MonetaryAmountDeserializer;
import ledger.util.MonetaryUtil;
import lombok.*;
import org.apache.commons.csv.CSVRecord;
import org.hibernate.annotations.UuidGenerator;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeneralLedgerActivity extends PanacheEntityBase {
    @Id
    @UuidGenerator
    private String id;
    private String activityId;
    private String loanId;

    private String commonName;

    private String activityType;

    private String transactionStrategy;

    @JsonDeserialize(using = MonetaryAmountDeserializer.class)
    private Double principal;

    @JsonDeserialize(using = MonetaryAmountDeserializer.class)
    private Double interest;

    @JsonDeserialize(using = MonetaryAmountDeserializer.class)
    private Double fee;

    @JsonDeserialize(using = MonetaryAmountDeserializer.class)
    private Double excess;

    private String direction;

    private String spread;

    private String reversalActivityType;

    private String reversalActivityId;
    LocalDateTime effectiveAt;
    LocalDateTime transactionTime;

    @Builder.Default
    private Double amount = 0.0;

    public MonetaryAmount getAmount() {
        return MonetaryUtil.toMonetaryAmount(amount);
    }

    public MonetaryAmount getPrincipal() {
        return MonetaryUtil.toMonetaryAmount(principal);
    }

    public MonetaryAmount getInterest() {
        return MonetaryUtil.toMonetaryAmount(interest);
    }

    public MonetaryAmount getFee() {
        return MonetaryUtil.toMonetaryAmount(fee);
    }

    public MonetaryAmount getExcess() {
        return MonetaryUtil.toMonetaryAmount(excess);
    }

    public void setAmount(MonetaryAmount amount) {
        this.amount = amount.getNumber().doubleValue();
    }

    public void setPrincipal(MonetaryAmount principal) {
        this.principal = principal.getNumber().doubleValue();
    }

    public void setInterest(MonetaryAmount interest) {
        this.interest = interest.getNumber().doubleValue();
    }

    public void setFee(MonetaryAmount fee) {
        this.fee = fee.getNumber().doubleValue();
    }

    public void setExcess(MonetaryAmount excess) {
        this.excess = excess.getNumber().doubleValue();
    }

    @SuppressWarnings("unused")
    public GeneralLedgerActivity(CSVRecord record) {
        this.loanId = record.get("loanId");
        this.activityId = record.get("activityId");
        this.commonName = record.get("commonName");
        this.activityType = record.get("activityType");
        this.transactionStrategy = record.get("transactionStrategy");
        this.direction = record.get("direction");
        this.spread = record.get("spread");
        this.reversalActivityType = record.get("reversalActivityType");
        this.reversalActivityId = record.get("reversalActivityId");
        this.setAmount(MonetaryUtil.toMonetaryAmount(record.get("amount")));
        this.effectiveAt = LocalDateTime.parse(record.get("effectiveAt"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T" + "'HH:mm:ss"));
        this.transactionTime = LocalDateTime.parse(record.get("transactionTime"),
                DateTimeFormatter.ofPattern("yyyy" + "-MM-dd'T" + "'HH:mm" + ":ss"));

        this.setPrincipal(MonetaryUtil.toMonetaryAmount(record.get("principal")));
        this.setInterest(MonetaryUtil.toMonetaryAmount(record.get("interest")));
        this.setFee(MonetaryUtil.toMonetaryAmount(record.get("fee")));
        this.setExcess(MonetaryUtil.toMonetaryAmount(record.get("excess")));
    }

    public boolean equals(LedgerActivity other) {
        return activityType.equals(other.getActivityType()) && activityId.equals(other.getActivityId());
    }
}
