package ledger.service;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.model.Balance;
import lombok.RequiredArgsConstructor;
import org.javamoney.moneta.Money;

import javax.money.MonetaryAmountFactory;

@ApplicationScoped
@RequiredArgsConstructor
public class BalanceService {

    private final MonetaryAmountFactory<Money> monetaryAmountFactory;

    public Balance add(Balance balance1, Balance balance2) {
        return new Balance(balance1.principal().add(balance2.principal()), balance1.interest().add(balance2.interest()),
                balance1.fees().add(balance2.fees()), balance1.excess().add(balance2.excess()));
    }

    public Balance subtract(Balance balance1, Balance balance2) {
        return new Balance(balance1.principal().subtract(balance2.principal()), balance1.interest().subtract(balance2.interest()),
                balance1.fees().subtract(balance2.fees()), balance1.excess().subtract(balance2.excess()));
    }


    public Balance createBalance(double principal, double interest, double fee, double excess, String currencyCode) {
        return new Balance(monetaryAmountFactory.setCurrency(currencyCode).setNumber(principal).create(),
                monetaryAmountFactory.setCurrency(currencyCode).setNumber(interest).create(),
                monetaryAmountFactory.setCurrency(currencyCode).setNumber(fee).create(),
                monetaryAmountFactory.setCurrency(currencyCode).setNumber(excess).create());
    }
}
