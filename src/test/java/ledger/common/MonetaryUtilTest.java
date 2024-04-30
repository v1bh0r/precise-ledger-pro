package ledger.common;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.money.MonetaryAmount;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonetaryUtilTest {

    @Test
    @DisplayName("Should convert double to MonetaryAmount with default currency")
    void shouldConvertDoubleToMonetaryAmountWithDefaultCurrency() {
        MonetaryAmount result = MonetaryUtil.toMonetaryAmount(100.0);

        assertEquals(Money.of(100.0, "USD"), result);
    }

    @Test
    @DisplayName("Should convert double to MonetaryAmount with specified currency")
    void shouldConvertDoubleToMonetaryAmountWithSpecifiedCurrency() {
        MonetaryAmount result = MonetaryUtil.toMonetaryAmount(100.0, "EUR");

        assertEquals(Money.of(100.0, "EUR"), result);
    }

    @Test
    @DisplayName("Should return zero MonetaryAmount")
    void shouldReturnZeroMonetaryAmount() {
        MonetaryAmount result = MonetaryUtil.zero();

        assertEquals(Money.of(0.0, "USD"), result);
    }

    @Test
    @DisplayName("Should convert MonetaryAmount to double")
    void shouldConvertMonetaryAmountToDouble() {
        MonetaryAmount amount = Money.of(100.0, "USD");

        double result = MonetaryUtil.toDouble(amount);

        assertEquals(100.0, result);
    }

    @Test
    @DisplayName("Should convert string to MonetaryAmount")
    void shouldConvertStringToMonetaryAmount() {
        MonetaryAmount result = MonetaryUtil.toMonetaryAmount("100.0");

        assertEquals(Money.of(100.0, "USD"), result);
    }

    @Test
    @DisplayName("Should return zero MonetaryAmount for non-numeric string")
    void shouldReturnZeroMonetaryAmountForNonNumericString() {
        MonetaryAmount result = MonetaryUtil.toMonetaryAmount("not a number");

        assertEquals(Money.of(0.0, "USD"), result);
    }

    @Test
    @DisplayName("Should format number correctly")
    void shouldFormatNumberCorrectly() {
        MonetaryAmount amount = Money.of(1000.0, "USD");

        String result = MonetaryUtil.formatNumber(amount);

        assertEquals("1.0K", result);
    }

    @Test
    @DisplayName("Should format negative number correctly")
    void shouldFormatNegativeNumberCorrectly() {
        MonetaryAmount amount = Money.of(-1000.0, "USD");

        String result = MonetaryUtil.formatNumber(amount);

        assertEquals("-1.0K", result);
    }
}