package ledger.util;

import ledger.producer.MonetaryProducer;
import org.javamoney.moneta.Money;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.MonetaryAmountFactory;

public class MonetaryUtil {
    private static final MonetaryAmountFactory<Money> monetaryAmountFactory =
            new MonetaryProducer().produceMonetaryAmountFactory();
    public static final String DEFAULT_CURRENCY_CODE = "USD";

    public static String getDefaultCurrencyCode() {
        return DEFAULT_CURRENCY_CODE;
    }

    public static MonetaryAmount toMonetaryAmount(double amount, String currencyCode) {
        return monetaryAmountFactory.setCurrency(currencyCode).setNumber(amount).create();
    }

    public static MonetaryAmount toMonetaryAmount(double amount) {
        return toMonetaryAmount(amount, DEFAULT_CURRENCY_CODE);
    }

    public static MonetaryAmount zero() {
        return toMonetaryAmount(0);
    }

    public static double toDouble(MonetaryAmount monetaryAmount) {
        return monetaryAmount.with(Monetary.getDefaultRounding()).getNumber().doubleValue();
    }

    public static MonetaryAmount toMonetaryAmount(String doubleValueAsString) {
        return toMonetaryAmount(safeParseDouble(doubleValueAsString));
    }

    private static double safeParseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static String formatNumber(MonetaryAmount amount) {
        var number = toDouble(amount);
        boolean isNegative = number < 0;

        number = Math.abs(number); // Take the absolute value for further processing

        // Define suffixes for large numbers
        String[] suffixes = {"", "K", "M", "B", "T"};

        int magnitude = 0;

        // Increase magnitude until the number is less than 1000
        while (number >= 1000 && magnitude < suffixes.length - 1) {
            number /= 1000;
            magnitude++;
        }

        // Format the number to 1 decimal place
        String formattedNumber = String.format("%.1f", number);

        // Combine with the appropriate suffix
        formattedNumber += suffixes[magnitude];

        // Reintroduce the negative sign if needed
        if (isNegative) {
            formattedNumber = "-" + formattedNumber;
        }

        return formattedNumber;
    }
}
