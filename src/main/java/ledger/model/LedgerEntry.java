package ledger.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvDate;
import ledger.common.MonetaryAmountCSVConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;

import static ledger.common.MonetaryUtil.zero;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LedgerEntry {
    @CsvBindByName
    private String loanId;
    @CsvBindByName
    private String entryId;
    @CsvBindByName
    private String entryType;

    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    @Builder.Default
    private MonetaryAmount amount = zero();

    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    @Builder.Default
    private MonetaryAmount principal = zero();

    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    @Builder.Default
    private MonetaryAmount interest = zero();

    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    @Builder.Default
    private MonetaryAmount fee = zero();

    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    @Builder.Default
    private MonetaryAmount excess = zero();

    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    @Builder.Default
    private MonetaryAmount principalBalance = zero();

    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    @Builder.Default
    private MonetaryAmount interestBalance = zero();

    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    @Builder.Default
    private MonetaryAmount feeBalance = zero();

    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    @Builder.Default
    private MonetaryAmount excessBalance = zero();

    @CsvBindByName
    @CsvDate(value = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime effectiveAt;
    @CsvBindByName
    @CsvDate(value = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @CsvBindByName
    private String sourceLedgerActivityType;
    @CsvBindByName
    private String sourceLedgerActivityId;

    public Balance getBalance() {
        return new Balance(principalBalance, interestBalance, feeBalance, excessBalance);
    }

    public Balance getBalanceChange() {
        return new Balance(principal, interest, fee, excess);
    }

    public String toString() {
        return entryId + ", " + entryType + ", " + principal + ", " + interest + ", " + fee + ", " + excess + ", " + effectiveAt + ", " + createdAt + ", " + sourceLedgerActivityType + ", " + sourceLedgerActivityId;
    }
}