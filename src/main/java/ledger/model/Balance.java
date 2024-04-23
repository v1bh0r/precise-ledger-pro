package ledger.model;

import javax.money.MonetaryAmount;

public record Balance(MonetaryAmount principal, MonetaryAmount interest, MonetaryAmount fee, MonetaryAmount excess) {
    public MonetaryAmount get(BalanceComponent component) {
        return switch (component) {
            case PRINCIPAL -> principal;
            case INTEREST -> interest;
            case FEES -> fee;
            case EXCESS -> excess;
            default -> throw new IllegalArgumentException("Unknown component: " + component);
        };
    }

    public Balance add(Balance balance) {
        return new Balance(this.principal().add(balance.principal()), this.interest().add(balance.interest()),
                this.fee().add(balance.fee()), this.excess().add(balance.excess()));
    }

    public Balance subtract(Balance balance) {
        return new Balance(this.principal().subtract(balance.principal()), this.interest().subtract(balance.interest()),
                this.fee().subtract(balance.fee()), this.excess().subtract(balance.excess()));
    }

    public boolean equals(Balance balance) {
        return this.principal.equals(balance.principal()) && this.interest.equals(balance.interest())
                && this.fee.equals(balance.fee()) && this.excess.equals(balance.excess());
    }

    public Balance negate() {
        return new Balance(this.principal().negate(), this.interest().negate(), this.fee().negate(), this.excess().negate());
    }

    public MonetaryAmount getTotalAmount() {
        return this.principal.add(this.interest).add(this.fee).add(this.excess);
    }
}
