package ledger.common.ledgeractivity.temporalactivity;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ledger.common.Ledger;
import ledger.common.ledgeractivity.domain.InterestRate;
import ledger.common.ledgeractivity.temporalactivity.command.DailyInterestCalculationCommand;
import ledger.model.Balance;
import ledger.model.LedgerClock;
import ledger.service.BalanceService;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import javax.money.Monetary;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ledger.common.MonetaryUtil.toMonetaryAmount;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class StartOfDayTest {
    private static final String CURRENCY = "USD";
    @Inject
    BalanceService balanceService;

    @Inject
    Logger log;

    @Inject
    DailyInterestCalculationCommand dailyInterestCalculationCommand;

    private static final String LOAN_ID = "12345";

    private Ledger createLedger(Balance startingBalance) {
        return new Ledger(LOAN_ID, startingBalance, new ArrayList<>(), CURRENCY);
    }

    @Test
    void applyTo() {
        var ledger = createLedger(balanceService.createBalance(10000, 100, 10, 0, CURRENCY));
        var temporalContext = new TemporalActivityContext();

        var interestRate = new InterestRate();
        interestRate.setRate(0.10f);
        interestRate.setId("1");
        interestRate.setEffectiveAt(LocalDateTime.MIN);
        interestRate.setLoanId(LOAN_ID);
        temporalContext.setProperty("interestRates", List.of(interestRate));
        temporalContext.setProperty("daysInYear", 365);
        temporalContext.setProperty("currencyCode", CURRENCY);

        var sod = new StartOfDay(LOAN_ID, "SOD", LocalDate.now().atStartOfDay().plusHours(5), temporalContext);
        sod.applyTo(ledger, new LedgerClock());
        // Expect an entry to be added to the ledger having interest accrual at the start of the day
        var entries = ledger.getEntries();
        assertEquals(1, entries.size());
        var entry = entries.getFirst();
        var balance = entry.getBalance();
        assertEquals(toMonetaryAmount(10000), balance.principal());
        assertEquals(toMonetaryAmount(102.74), balance.interest().with(Monetary.getDefaultRounding()));
    }
}