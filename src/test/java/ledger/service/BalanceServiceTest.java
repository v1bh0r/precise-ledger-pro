package ledger.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class BalanceServiceTest {
    @Inject
    BalanceService balanceService;

    @Test
    @DisplayName("Should create balance with correct values")
    void shouldCreateBalanceWithCorrectValues() {
        var balance = balanceService.createBalance(100.0, 10.0, 5.0, 0.0, "USD");

        assertEquals(100.0, balance.principal().getNumber().doubleValue());
        assertEquals(10.0, balance.interest().getNumber().doubleValue());
        assertEquals(5.0, balance.fee().getNumber().doubleValue());
        assertEquals(0.0, balance.excess().getNumber().doubleValue());
    }

    @Test
    @DisplayName("Should create balance with zero values")
    void shouldCreateBalanceWithZeroValues() {
        var balance = balanceService.createBalance(0.0, 0.0, 0.0, 0.0, "USD");

        assertEquals(0.0, balance.principal().getNumber().doubleValue());
        assertEquals(0.0, balance.interest().getNumber().doubleValue());
        assertEquals(0.0, balance.fee().getNumber().doubleValue());
        assertEquals(0.0, balance.excess().getNumber().doubleValue());
    }

    @Test
    @DisplayName("Should create balance with negative values")
    void shouldCreateBalanceWithNegativeValues() {
        var balance = balanceService.createBalance(-100.0, -10.0, -5.0, -1.0, "USD");

        assertEquals(-100.0, balance.principal().getNumber().doubleValue());
        assertEquals(-10.0, balance.interest().getNumber().doubleValue());
        assertEquals(-5.0, balance.fee().getNumber().doubleValue());
        assertEquals(-1.0, balance.excess().getNumber().doubleValue());
    }
}