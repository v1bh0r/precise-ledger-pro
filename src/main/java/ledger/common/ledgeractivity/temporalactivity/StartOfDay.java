package ledger.common.ledgeractivity.temporalactivity;

import ledger.common.Ledger;
import ledger.common.ledgeractivity.TemporalActivity;
import ledger.common.ledgeractivity.temporalactivity.command.DailyInterestCalculationCommand;
import ledger.model.GeneralLedgerActivity;
import ledger.model.LedgerClock;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.List;

import static ledger.service.LedgerEntryIdService.generateId;

public class StartOfDay extends TemporalActivity {
    private static final String ACTIVITY_TYPE = "StartOfDay";
    @NonNull
    private final LocalDateTime sodDateTime;
    @NonNull
    private final TemporalActivityContext sodContext;

    private final List<String> temporalActivityCommands =
            List.of(DailyInterestCalculationCommand.class.getSimpleName());

    public StartOfDay(@NonNull String loanId, @NonNull String commonName, @NonNull LocalDateTime sodDateTime,
                      TemporalActivityContext temporalActivityContext) {
        super(loanId, commonName, ACTIVITY_TYPE, getID(sodDateTime), sodDateTime, LocalDateTime.now());
        this.sodDateTime = sodDateTime;
        this.sodContext = temporalActivityContext == null ? new TemporalActivityContext() : temporalActivityContext;
    }

    public StartOfDay(TemporalActivityContext temporalActivityContext,
                      @NonNull GeneralLedgerActivity generalLedgerActivity) {
        super(generalLedgerActivity.getLoanId(), generalLedgerActivity.getCommonName(),
                generalLedgerActivity.getActivityType(),
                generalLedgerActivity.getActivityId(), generalLedgerActivity.getEffectiveAt(),
                generalLedgerActivity.getCreatedAt());
        this.sodDateTime = generalLedgerActivity.getEffectiveAt();
        this.sodContext = temporalActivityContext == null ? new TemporalActivityContext() : temporalActivityContext;

    }

    private static String getID(LocalDateTime sodDateTime) {
        return sodDateTime.toString();
    }

    private String getID() {
        return getID(sodDateTime);
    }

    @Override
    public void generateLedgerEntries(Ledger ledger,
                                      LedgerClock ledgerClock) {
        var balance = ledger.getCurrentBalance();
        temporalActivityCommands.forEach(commandName -> {
            var nextLedgerEntryId = generateId();
            var command = TemporalActivityCommandFactory.getCommand(commandName);
            ledger.addEntry(command.execute(nextLedgerEntryId, ledger.getLoanId(), balance,
                    getActivityType(), getActivityId(), sodDateTime, sodContext));
        });
    }
}
