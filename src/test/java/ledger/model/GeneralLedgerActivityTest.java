package ledger.model;

import org.apache.commons.csv.CSVRecord;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ledger.util.MonetaryUtil.getDefaultCurrencyCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneralLedgerActivityTest {

    @Test
    @DisplayName("Should create GeneralLedgerActivity from CSVRecord correctly")
    void shouldCreateGeneralLedgerActivityFromCSVRecordCorrectly() {
        CSVRecord record = mock(CSVRecord.class);
        when(record.get("loanId")).thenReturn("1234");
        when(record.get("activityId")).thenReturn("5678");
        when(record.get("commonName")).thenReturn("Test Activity");
        when(record.get("activityType")).thenReturn("Test Type");
        when(record.get("transactionStrategy")).thenReturn("Test Strategy");
        when(record.get("direction")).thenReturn("Test Direction");
        when(record.get("spread")).thenReturn("Test Spread");
        when(record.get("reversalActivityType")).thenReturn("Test Reversal Type");
        when(record.get("reversalActivityId")).thenReturn("9012");
        when(record.get("amount")).thenReturn("100.0");
        when(record.get("effectiveAt")).thenReturn("2022-01-01T00:00:00");
        when(record.get("transactionTime")).thenReturn("2022-01-01T00:00:00");
        when(record.get("principal")).thenReturn("100.0");
        when(record.get("interest")).thenReturn("10.0");
        when(record.get("fee")).thenReturn("5.0");
        when(record.get("excess")).thenReturn("0.0");

        GeneralLedgerActivity activity = new GeneralLedgerActivity(record);

        assertEquals("1234", activity.getLoanId());
        assertEquals("5678", activity.getActivityId());
        assertEquals("Test Activity", activity.getCommonName());
        assertEquals("Test Type", activity.getActivityType());
        assertEquals("Test Strategy", activity.getTransactionStrategy());
        assertEquals("Test Direction", activity.getDirection());
        assertEquals("Test Spread", activity.getSpread());
        assertEquals("Test Reversal Type", activity.getReversalActivityType());
        assertEquals("9012", activity.getReversalActivityId());
        assertEquals(Money.of(100.0, getDefaultCurrencyCode()), activity.getAmount());
        assertEquals(LocalDateTime.parse("2022-01-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                activity.getEffectiveAt());
        assertEquals(LocalDateTime.parse("2022-01-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                activity.getTransactionTime());
        assertEquals(Money.of(100.0, getDefaultCurrencyCode()), activity.getPrincipal());
        assertEquals(Money.of(10.0, getDefaultCurrencyCode()), activity.getInterest());
        assertEquals(Money.of(5.0, getDefaultCurrencyCode()), activity.getFee());
        assertEquals(Money.of(0.0, getDefaultCurrencyCode()), activity.getExcess());
    }
}