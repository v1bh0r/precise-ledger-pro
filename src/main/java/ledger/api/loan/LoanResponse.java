package ledger.api.loan;

import lombok.Data;

import java.util.UUID;

@Data
public class LoanResponse {
    private UUID id;
    private Integer daysInYear;
    private String currencyCode;
    private String externalId;
}
