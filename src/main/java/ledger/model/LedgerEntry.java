package ledger.model;

import ledger.common.MonetaryUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.csv.CSVRecord;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ledger.common.MonetaryUtil.formatNumber;
import static ledger.common.MonetaryUtil.zero;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LedgerEntry implements Cloneable {

    private String loanId;

    private String entryId;

    private String entryType;


    @Builder.Default
    private MonetaryAmount amount = zero();


    @Builder.Default
    private MonetaryAmount principal = zero();


    @Builder.Default
    private MonetaryAmount interest = zero();


    @Builder.Default
    private MonetaryAmount fee = zero();


    @Builder.Default
    private MonetaryAmount excess = zero();


    @Builder.Default
    private MonetaryAmount principalBalance = zero();


    @Builder.Default
    private MonetaryAmount interestBalance = zero();


    @Builder.Default
    private MonetaryAmount feeBalance = zero();


    @Builder.Default
    private MonetaryAmount excessBalance = zero();


    private LocalDateTime effectiveAt;


    private LocalDateTime createdAt;

    private String sourceLedgerActivityType;

    private String sourceLedgerActivityId;

    public Balance getBalance() {
        return new Balance(principalBalance, interestBalance, feeBalance, excessBalance);
    }

    public Balance getBalanceChange() {
        return new Balance(principal, interest, fee, excess);
    }

    public String toString() {
        return String.format("%-5s%-10s%-10s%-10s%-10s%-10s%-10s%-10s%20s%20s%12s%-4s", entryId,
                entryType.substring(0, 5)
                , formatNumber(principal), formatNumber(interest), formatNumber(fee), formatNumber(principalBalance),
                formatNumber(interestBalance), formatNumber(feeBalance), shortTimestamp(effectiveAt),
                shortTimestamp(createdAt), sourceLedgerActivityType, sourceLedgerActivityId);
    }

    private String shortTimestamp(LocalDateTime timestamp) {
        return timestamp.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }

    @SuppressWarnings("unused")
    public LedgerEntry(CSVRecord record) {
        this.loanId = record.get("loanId");
        this.entryId = record.get("entryId");
        this.entryType = record.get("entryType");
        this.amount = MonetaryUtil.toMonetaryAmount(record.get("amount"));
        this.principal = MonetaryUtil.toMonetaryAmount(record.get("principal"));
        this.interest = MonetaryUtil.toMonetaryAmount(record.get("interest"));
        this.fee = MonetaryUtil.toMonetaryAmount(record.get("fee"));
        this.excess = MonetaryUtil.toMonetaryAmount(record.get("excess"));
        this.principalBalance = MonetaryUtil.toMonetaryAmount(record.get("principalBalance"));
        this.interestBalance = MonetaryUtil.toMonetaryAmount(record.get("interestBalance"));
        this.feeBalance = MonetaryUtil.toMonetaryAmount(record.get("feeBalance"));
        this.excessBalance = MonetaryUtil.toMonetaryAmount(record.get("excessBalance"));
        this.effectiveAt = LocalDateTime.parse(record.get("effectiveAt"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T" + "'HH:mm:ss"));
        this.createdAt = LocalDateTime.parse(record.get("createdAt"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T" +
                "'HH:mm:ss"));
        this.sourceLedgerActivityType = record.get("sourceLedgerActivityType");
        this.sourceLedgerActivityId = record.get("sourceLedgerActivityId");
    }

    public void updateBalances(Balance currentLedgerBalance) {
        this.principalBalance = currentLedgerBalance.principal().add(principal);
        this.interestBalance = currentLedgerBalance.interest().add(interest);
        this.feeBalance = currentLedgerBalance.fee().add(fee);
        this.excessBalance = currentLedgerBalance.excess().add(excess);
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