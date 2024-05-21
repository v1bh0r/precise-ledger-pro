package ledger.model;

import ledger.util.MonetaryUtil;

import javax.money.MonetaryAmount;

import static ledger.util.MonetaryUtil.toDouble;

public record Balance(MonetaryAmount principal, MonetaryAmount interest, MonetaryAmount fee, MonetaryAmount excess) {
    public Balance(double principal, double interest, double fee, double excess) {
        this(MonetaryUtil.toMonetaryAmount(principal), MonetaryUtil.toMonetaryAmount(interest),
                MonetaryUtil.toMonetaryAmount(fee), MonetaryUtil.toMonetaryAmount(excess));
    }

    public MonetaryAmount get(BalanceComponent component) {
        return switch (component) {
            case PRINCIPAL -> principal;
            case INTEREST -> interest;
            case FEES -> fee;
            case EXCESS -> excess;
        };
    }

    public Balance add(Balance balance) {
        return new Balance(this.principal().add(balance.principal()), this.interest()
                .add(balance.interest()), this.fee().add(balance.fee()), this.excess().add(balance.excess()));
    }

    public Balance subtract(Balance balance) {
        return new Balance(this.principal().subtract(balance.principal()), this.interest()
                .subtract(balance.interest()), this.fee().subtract(balance.fee()), this.excess()
                .subtract(balance.excess()));
    }

    public boolean equals(Balance balance) {
        return toDouble(this.principal) == toDouble(balance.principal()) &&
                toDouble(this.interest) == toDouble(balance.interest()) &&
                toDouble(this.fee) == toDouble(balance.fee()) &&
                toDouble(this.excess) == toDouble(balance.excess());
    }

    public Balance negate() {
        return new Balance(this.principal().negate(), this.interest().negate(), this.fee().negate(), this.excess()
                .negate());
    }

    public MonetaryAmount getTotalAmount() {
        return this.principal.add(this.interest).add(this.fee).add(this.excess);
    }
}
