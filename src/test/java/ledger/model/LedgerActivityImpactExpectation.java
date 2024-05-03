package ledger.model;

import org.apache.commons.csv.CSVRecord;

public record LedgerActivityImpactExpectation(String activityId, String activityType, double principal, double interest,
                                              double fee, double excess, double principalBalance,
                                              double interestBalance, double feeBalance, double excessBalance) {
    public LedgerActivityImpactExpectation(CSVRecord record) {
        this(record.get("activityId"), record.get("activityType"), Double.parseDouble(record.get("principal")),
                Double.parseDouble(record.get("interest")), Double.parseDouble(record.get("fee")),
                Double.parseDouble(record.get("excess")), Double.parseDouble(record.get("principalBalance")),
                Double.parseDouble(record.get("interestBalance")), Double.parseDouble(record.get("feeBalance")),
                Double.parseDouble(record.get("excessBalance")));
    }

    public Balance getImpact() {
        return new Balance(principal, interest, fee, excess);
    }
}
