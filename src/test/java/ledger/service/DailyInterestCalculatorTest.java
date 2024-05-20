package ledger.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ledger.util.MonetaryUtil;
import org.junit.jupiter.api.Test;

import javax.money.Monetary;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class DailyInterestCalculatorTest {
    @Inject
    DailyInterestCalculator dailyInterestCalculator;

    @Test
    void calculateInterest() {
        assertEquals(MonetaryUtil.toMonetaryAmount(0.27),
                dailyInterestCalculator.calculateInterest(MonetaryUtil.toMonetaryAmount(1000), .10f, 365)
                        .with(Monetary.getDefaultRounding()));
    }
}