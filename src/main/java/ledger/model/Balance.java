package ledger.model;

import javax.money.MonetaryAmount;

public record Balance(MonetaryAmount principal, MonetaryAmount interest, MonetaryAmount fees, MonetaryAmount excess) {


    public MonetaryAmount get(BalanceComponent component) {
        return switch (component) {
            case PRINCIPAL -> principal;
            case INTEREST -> interest;
            case FEES -> fees;
            case EXCESS -> excess;
            default -> throw new IllegalArgumentException("Unknown component: " + component);
        };
    }
}
