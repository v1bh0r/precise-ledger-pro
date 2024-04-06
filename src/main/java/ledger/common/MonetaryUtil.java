package ledger.common;

import ledger.producer.MonetaryProducer;
import org.javamoney.moneta.Money;

import javax.money.MonetaryAmount;
import javax.money.MonetaryAmountFactory;

public class MonetaryUtil {
    private static final MonetaryAmountFactory<Money> monetaryAmountFactory = new MonetaryProducer().produceMonetaryAmountFactory();
    private static final String DEFAULT_CURRENCY_CODE = "USD";

    public static MonetaryAmount monetaryAmount(double amount, String currencyCode) {
        return monetaryAmountFactory.setCurrency(currencyCode).setNumber(amount).create();
    }

    public static MonetaryAmount monetaryAmount(double amount) {
        return monetaryAmount(amount, DEFAULT_CURRENCY_CODE);
    }

    public static MonetaryAmount zero() {
        return monetaryAmount(0);
    }
}
