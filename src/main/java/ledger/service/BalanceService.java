package ledger.service;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.model.Balance;
import lombok.RequiredArgsConstructor;
import org.javamoney.moneta.Money;

import javax.money.Monetary;
import javax.money.MonetaryAmountFactory;

@ApplicationScoped
@RequiredArgsConstructor
public class BalanceService {

    private final MonetaryAmountFactory<Money> monetaryAmountFactory;

    public Balance createBalance(double principal, double interest, double fee, double excess, String currencyCode) {
        return new Balance(monetaryAmountFactory.setCurrency(currencyCode).setNumber(principal).create(),
                monetaryAmountFactory.setCurrency(currencyCode).setNumber(interest).create(),
                monetaryAmountFactory.setCurrency(currencyCode).setNumber(fee).create(),
                monetaryAmountFactory.setCurrency(currencyCode).setNumber(excess).create());
    }

    public static Balance createZeroBalance(String currencyCode) {
        var currencyUnit = Monetary.getCurrency(currencyCode);
        return new Balance(Money.zero(currencyUnit), Money.zero(currencyUnit), Money.zero(currencyUnit),
                Money.zero(currencyUnit));
    }

}
