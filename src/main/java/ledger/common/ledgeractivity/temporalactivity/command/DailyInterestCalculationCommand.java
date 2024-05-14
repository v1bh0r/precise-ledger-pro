package ledger.common.ledgeractivity.temporalactivity.command;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.ledgeractivity.domain.InterestRate;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityCommand;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityCommandFactory;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityContext;
import ledger.model.Balance;
import ledger.model.LedgerEntry;
import ledger.service.DailyInterestCalculator;
import lombok.AllArgsConstructor;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

import static ledger.common.MonetaryUtil.toDouble;

@Startup
@ApplicationScoped
@AllArgsConstructor
public class DailyInterestCalculationCommand implements TemporalActivityCommand {
    private final Logger logger;

    private final DailyInterestCalculator dailyInterestCalculator;

    @PostConstruct
    void onInit() {
        TemporalActivityCommandFactory.register(this);
    }

    @Override
    public LedgerEntry execute(String nextLedgerEntryId, String loanId, Balance loanBalance, String activityType,
                               String activityId, LocalDateTime effectiveAt, TemporalActivityContext context) {
        logger.info("Executing DailyInterestCalculationCommand for loanId: " + loanId);

        List<InterestRate> interestRates = context.getListProperty("interestRates", InterestRate.class);
        var daysInYear = context.getProperty("daysInYear", Integer.class);
        var interest = dailyInterestCalculator.calculateInterest(loanBalance.principal(), interestRates, daysInYear,
                effectiveAt);

        return new LedgerEntry(loanId, null, "Interest Accrual", toDouble(interest),
                0.0, toDouble(interest),
                0.0, 0.0,
                toDouble(loanBalance.principal()), toDouble(loanBalance.interest()
                .add(interest)), toDouble(loanBalance.fee()), toDouble(loanBalance.excess()), effectiveAt,
                LocalDateTime.now(),
                activityType, activityId);
    }
}
