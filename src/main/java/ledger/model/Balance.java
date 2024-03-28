package ledger.model;

import javax.money.MonetaryAmount;

public record Balance(MonetaryAmount principal, MonetaryAmount interest, MonetaryAmount fees, MonetaryAmount excess) {

    public Balance add(Balance other) {
        return new Balance(principal.add(other.principal), interest.add(other.interest), fees.add(other.fees), excess.add(other.excess));
    }

    public Balance subtract(Balance other) {
        return new Balance(principal.subtract(other.principal), interest.subtract(other.interest), fees.subtract(other.fees), excess.subtract(other.excess));
    }

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
