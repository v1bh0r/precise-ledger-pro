package ledger.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import ledger.common.MonetaryUtil;
import lombok.*;
import org.apache.commons.csv.CSVRecord;
import org.hibernate.annotations.UuidGenerator;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ledger.common.MonetaryUtil.formatNumber;
import static ledger.common.MonetaryUtil.toDouble;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
public class LedgerEntry extends PanacheEntityBase implements Cloneable {
    @NonNull
    private String loanId;
    @Id
    @UuidGenerator
    private String entryId;

    @NonNull
    private String entryType;

    @NonNull
    @Builder.Default
    private Double amount = 0.0;

    public MonetaryAmount getAmount() {
        return MonetaryUtil.toMonetaryAmount(amount);
    }

    @NonNull
    @Builder.Default
    private Double principal = 0.0;

    public MonetaryAmount getPrincipal() {
        return MonetaryUtil.toMonetaryAmount(principal);
    }

    @NonNull
    @Builder.Default
    private Double interest = 0.0;

    public MonetaryAmount getInterest() {
        return MonetaryUtil.toMonetaryAmount(interest);
    }

    @NonNull
    @Builder.Default
    private Double fee = 0.0;

    public MonetaryAmount getFee() {
        return MonetaryUtil.toMonetaryAmount(fee);
    }

    @NonNull
    @Builder.Default
    private Double excess = 0.0;

    public MonetaryAmount getExcess() {
        return MonetaryUtil.toMonetaryAmount(excess);
    }

    @NonNull
    @Builder.Default
    private Double principalBalance = 0.0;

    public MonetaryAmount getPrincipalBalance() {
        return MonetaryUtil.toMonetaryAmount(principalBalance);
    }

    @NonNull
    @Builder.Default
    private Double interestBalance = 0.0;

    public MonetaryAmount getInterestBalance() {
        return MonetaryUtil.toMonetaryAmount(interestBalance);
    }

    @NonNull
    @Builder.Default
    private Double feeBalance = 0.0;

    public MonetaryAmount getFeeBalance() {
        return MonetaryUtil.toMonetaryAmount(feeBalance);
    }

    @NonNull
    @Builder.Default
    private Double excessBalance = 0.0;

    public MonetaryAmount getExcessBalance() {
        return MonetaryUtil.toMonetaryAmount(excessBalance);
    }

    @NonNull
    private LocalDateTime effectiveAt;

    @NonNull
    private LocalDateTime createdAt;

    @NonNull
    private String sourceLedgerActivityType;

    @NonNull
    private String sourceLedgerActivityId;

    public Balance getBalance() {
        return new Balance(principalBalance, interestBalance, feeBalance, excessBalance);
    }

    public Balance getBalanceChange() {
        return new Balance(principal, interest, fee, excess);
    }

    public String toString() {
        return String.format("%-5s%-10s%-10s%-10s%-10s%-10s%-10s%-10s%20s%20s%12s%-4s", entryId,
                entryType.substring(0, 5), formatNumber(getPrincipal()), formatNumber(getInterest()),
                formatNumber(getFee()),
                formatNumber(getPrincipalBalance()), formatNumber(getInterestBalance()), formatNumber(getFeeBalance()),
                shortTimestamp(effectiveAt), shortTimestamp(createdAt), sourceLedgerActivityType,
                sourceLedgerActivityId);
    }

    private String shortTimestamp(LocalDateTime timestamp) {
        return timestamp.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }

    @SuppressWarnings("unused")
    public LedgerEntry(CSVRecord record) {
        this.loanId = record.get("loanId");
        this.entryType = record.get("entryType");
        this.amount = Double.parseDouble(record.get("amount"));
        this.principal = Double.parseDouble(record.get("principal"));
        this.interest = Double.parseDouble(record.get("interest"));
        this.fee = Double.parseDouble(record.get("fee"));
        this.excess = Double.parseDouble(record.get("excess"));
        this.principalBalance = Double.parseDouble(record.get("principalBalance"));
        this.interestBalance = Double.parseDouble(record.get("interestBalance"));
        this.feeBalance = Double.parseDouble(record.get("feeBalance"));
        this.excessBalance = Double.parseDouble(record.get("excessBalance"));
        this.effectiveAt = LocalDateTime.parse(record.get("effectiveAt"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T" + "'HH:mm:ss"));
        this.createdAt = LocalDateTime.parse(record.get("createdAt"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T" +
                "'HH:mm:ss"));
        this.sourceLedgerActivityType = record.get("sourceLedgerActivityType");
        this.sourceLedgerActivityId = record.get("sourceLedgerActivityId");
    }

    public void updateBalances(Balance currentLedgerBalance) {
        this.principalBalance = toDouble(currentLedgerBalance.principal().add(getPrincipal()));
        this.interestBalance = toDouble(currentLedgerBalance.interest().add(getInterest()));
        this.feeBalance = toDouble(currentLedgerBalance.fee().add(getFee()));
        this.excessBalance = toDouble(currentLedgerBalance.excess().add(getExcess()));
    }

    @Override
    public LedgerEntry clone() {
        try {
            return (LedgerEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}