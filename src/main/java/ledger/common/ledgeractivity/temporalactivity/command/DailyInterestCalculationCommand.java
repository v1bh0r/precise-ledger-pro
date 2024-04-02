package ledger.common.ledgeractivity.temporalactivity.command;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import ledger.common.ledgeractivity.domain.InterestRate;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityCommand;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityCommandFactory;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityContext;
import ledger.model.Balance;
import ledger.model.LedgerEntry;
import ledger.service.DailyInterestCalculator;
import lombok.AllArgsConstructor;
import org.javamoney.moneta.Money;
import org.jboss.logging.Logger;

import javax.money.Monetary;
import java.time.LocalDateTime;
import java.util.List;

@Singleton
@AllArgsConstructor
public class DailyInterestCalculationCommand implements TemporalActivityCommand {
    private final Logger logger;

    private final DailyInterestCalculator dailyInterestCalculator;

    @PostConstruct
    void onInit() {
        TemporalActivityCommandFactory.register(this);
    }

    @Override
    public LedgerEntry execute(String loanId, Balance loanBalance, String activityType, String activityId, LocalDateTime effectiveAt, TemporalActivityContext context) {
        logger.info("Executing DailyInterestCalculationCommand for loanId: " + loanId);

        List<InterestRate> interestRates = context.getListProperty("interestRates", InterestRate.class);
        var daysInYear = context.getProperty("daysInYear", Integer.class);
        var interest = dailyInterestCalculator.calculateInterest(loanBalance.principal(), interestRates, daysInYear, effectiveAt);
        var currencyCode = context.getProperty("currencyCode", String.class);

        return new LedgerEntry(loanId,
                effectiveAt.toLocalDate().toString(),
                "Interest Accrual",
                interest,
                Money.zero(Monetary.getCurrency(currencyCode)),
                interest,
                Money.zero(Monetary.getCurrency(currencyCode)),
                Money.zero(Monetary.getCurrency(currencyCode)),
                loanBalance.principal(),
                loanBalance.interest().add(interest),
                loanBalance.fee(),
                loanBalance.excess(),
                effectiveAt,
                LocalDateTime.now(),
                activityType, activityId);
    }
}
