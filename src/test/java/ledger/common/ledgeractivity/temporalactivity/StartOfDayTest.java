package ledger.common.ledgeractivity.temporalactivity;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ledger.common.Ledger;
import ledger.common.ledgeractivity.domain.InterestRate;
import ledger.model.Balance;
import ledger.model.LedgerClock;
import ledger.service.BalanceService;
import org.junit.jupiter.api.Test;

import javax.money.Monetary;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ledger.config.AppConfig.DEFAULT_CURRENCY_CODE;
import static ledger.config.AppConfig.DEFAULT_DAYS_IN_YEAR;
import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;
import static ledger.util.MonetaryUtil.toMonetaryAmount;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class StartOfDayTest {
    @Inject
    BalanceService balanceService;

    private static final String LOAN_ID = "12345";

    private Ledger createLedger(Balance startingBalance) {
        return new Ledger(LOAN_ID, startingBalance, new ArrayList<>(), DEFAULT_CURRENCY_CODE);
    }

    @Test
    void applyTo() {
        var ledger = createLedger(balanceService.createBalance(10000, 100, 10, 0, DEFAULT_CURRENCY_CODE));
        var temporalContext = new TemporalActivityContext();

        var interestRate = new InterestRate();
        interestRate.setRate(0.10f);
        interestRate.setId(UUID.randomUUID());
        interestRate.setEffectiveAt(DB_SAFE_LOCAL_DATETIME_MIN);
        interestRate.setLoanId(LOAN_ID);
        temporalContext.setProperty("interestRates", List.of(interestRate));
        temporalContext.setProperty("daysInYear", DEFAULT_DAYS_IN_YEAR);
        temporalContext.setProperty("currencyCode", DEFAULT_CURRENCY_CODE);

        var sod = new StartOfDay(LOAN_ID, "SOD", LocalDate.now().atStartOfDay().plusHours(5));
        sod.applyTo(ledger, new LedgerClock(), temporalContext);
        // Expect an entry to be added to the ledger having interest accrual at the start of the day
        var entries = ledger.getEntries();
        assertEquals(1, entries.size());
        var entry = entries.getFirst();
        var balance = entry.getBalance();
        assertEquals(toMonetaryAmount(10000), balance.principal());
        assertEquals(toMonetaryAmount(102.74), balance.interest().with(Monetary.getDefaultRounding()));
    }
}