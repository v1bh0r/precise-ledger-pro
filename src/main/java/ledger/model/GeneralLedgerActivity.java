package ledger.model;

// activityId,commonName,activityType,transactionStrategy,direction,spread,reversalActivityType,reversalActivityId,amount,effectiveAt,createdAt

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvDate;
import ledger.common.MonetaryAmountCSVConverter;
import ledger.common.MonetaryUtil;
import lombok.*;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeneralLedgerActivity {
    @CsvBindByName
    private String loanId;
    @CsvBindByName
    private String activityId;
    @CsvBindByName
    private String commonName;
    @CsvBindByName
    private String activityType;
    @CsvBindByName
    private String transactionStrategy;
    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    private MonetaryAmount principal = MonetaryUtil.zero();
    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    private MonetaryAmount interest = MonetaryUtil.zero();
    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    private MonetaryAmount fee = MonetaryUtil.zero();
    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    private MonetaryAmount excess = MonetaryUtil.zero();
    @CsvBindByName
    private String direction;
    @CsvBindByName
    private String spread;
    @CsvBindByName
    private String reversalActivityType;
    @CsvBindByName
    private String reversalActivityId;
    @CsvCustomBindByName(converter = MonetaryAmountCSVConverter.class)
    @Builder.Default
    private MonetaryAmount amount = MonetaryUtil.zero();
    @CsvBindByName
    @CsvDate(value = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime effectiveAt;
    @CsvDate(value = "yyyy-MM-dd'T'HH:mm:ss")
    @CsvBindByName
    LocalDateTime createdAt;
}
