package ledger.model;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LedgerEntryTest {

    @Test
    @DisplayName("Should create LedgerEntry correctly")
    void shouldCreateLedgerEntryCorrectly() {
        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .loanId("1234")
                .entryType("Test Type")
                .amount(100.0)
                .principal(100.0)
                .interest(10.0)
                .fee(5.0)
                .excess(0.0)
                .principalBalance(100.0)
                .interestBalance(10.0)
                .feeBalance(5.0)
                .excessBalance(0.0)
                .effectiveAt(LocalDateTime.parse("2022-01-01T00:00:00"))
                .createdAt(LocalDateTime.parse("2022-01-01T00:00:00"))
                .sourceLedgerActivityType("Test Activity Type")
                .sourceLedgerActivityId("9012")
                .build();

        assertEquals("1234", ledgerEntry.getLoanId());
        assertEquals("5678", ledgerEntry.getEntryId());
        assertEquals("Test Type", ledgerEntry.getEntryType());
        assertEquals(Money.of(100.0, "USD"), ledgerEntry.getAmount());
        assertEquals(Money.of(100.0, "USD"), ledgerEntry.getPrincipal());
        assertEquals(Money.of(10.0, "USD"), ledgerEntry.getInterest());
        assertEquals(Money.of(5.0, "USD"), ledgerEntry.getFee());
        assertEquals(Money.of(0.0, "USD"), ledgerEntry.getExcess());
        assertEquals(Money.of(100.0, "USD"), ledgerEntry.getPrincipalBalance());
        assertEquals(Money.of(10.0, "USD"), ledgerEntry.getInterestBalance());
        assertEquals(Money.of(5.0, "USD"), ledgerEntry.getFeeBalance());
        assertEquals(Money.of(0.0, "USD"), ledgerEntry.getExcessBalance());
        assertEquals(LocalDateTime.parse("2022-01-01T00:00:00"), ledgerEntry.getEffectiveAt());
        assertEquals(LocalDateTime.parse("2022-01-01T00:00:00"), ledgerEntry.getCreatedAt());
        assertEquals("Test Activity Type", ledgerEntry.getSourceLedgerActivityType());
        assertEquals("9012", ledgerEntry.getSourceLedgerActivityId());
    }

    @Test
    @DisplayName("Should update balances correctly")
    void shouldUpdateBalancesCorrectly() {
        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .loanId("1234")
                .principal(100.0)
                .interest(10.0)
                .fee(5.0)
                .excess(0.0)
                .entryType("Test Type")
                .amount(100.0)
                .principalBalance(100.0)
                .interestBalance(10.0)
                .feeBalance(5.0)
                .excessBalance(0.0)
                .effectiveAt(LocalDateTime.parse("2022-01-01T00:00:00"))
                .createdAt(LocalDateTime.parse("2022-01-01T00:00:00"))
                .sourceLedgerActivityType("Test Activity Type")
                .sourceLedgerActivityId("9012")
                .build();

        Balance currentLedgerBalance = new Balance(Money.of(200.0, "USD"), Money.of(20.0, "USD"), Money.of(10.0, "USD"
        ), Money.of(0.0, "USD"));

        ledgerEntry.updateBalances(currentLedgerBalance);

        assertEquals(Money.of(300.0, "USD"), ledgerEntry.getPrincipalBalance());
        assertEquals(Money.of(30.0, "USD"), ledgerEntry.getInterestBalance());
        assertEquals(Money.of(15.0, "USD"), ledgerEntry.getFeeBalance());
        assertEquals(Money.of(0.0, "USD"), ledgerEntry.getExcessBalance());
    }
}