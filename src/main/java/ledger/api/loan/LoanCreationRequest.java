package ledger.api.loan;

import lombok.Data;

@Data
public class LoanCreationRequest {
    private Integer daysInYear;
    private String currencyCode;
    private String externalId;
}
