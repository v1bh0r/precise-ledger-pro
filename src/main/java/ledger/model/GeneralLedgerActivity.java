package ledger.model;

import ledger.common.MonetaryUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.csv.CSVRecord;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
@AllArgsConstructor
public class GeneralLedgerActivity {

    private String loanId;

    private String activityId;

    private String commonName;

    private String activityType;

    private String transactionStrategy;

    private MonetaryAmount principal;

    private MonetaryAmount interest;

    private MonetaryAmount fee;

    private MonetaryAmount excess;

    private String direction;

    private String spread;

    private String reversalActivityType;

    private String reversalActivityId;

    @Builder.Default
    private MonetaryAmount amount = MonetaryUtil.zero();

    LocalDateTime effectiveAt;

    LocalDateTime createdAt;

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
        this.amount = MonetaryUtil.toMonetaryAmount(record.get("amount"));
        this.effectiveAt = LocalDateTime.parse(record.get("effectiveAt"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T" + "'HH:mm:ss"));
        this.createdAt = LocalDateTime.parse(record.get("createdAt"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"
                + ":ss"));

        this.principal = MonetaryUtil.toMonetaryAmount(record.get("principal"));
        this.interest = MonetaryUtil.toMonetaryAmount(record.get("interest"));
        this.fee = MonetaryUtil.toMonetaryAmount(record.get("fee"));
        this.excess = MonetaryUtil.toMonetaryAmount(record.get("excess"));
    }
}
