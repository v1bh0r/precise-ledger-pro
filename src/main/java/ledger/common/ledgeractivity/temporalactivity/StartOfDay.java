package ledger.common.ledgeractivity.temporalactivity;

import ledger.common.Ledger;
import ledger.common.ledgeractivity.TemporalActivity;
import ledger.common.ledgeractivity.temporalactivity.command.DailyInterestCalculationCommand;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StartOfDay extends TemporalActivity {
    private static final String ACTIVITY_TYPE = "StartOfDay";
    private final LocalDateTime sodDateTime;
    private final TemporalActivityContext sodContext;
    private final List<String> temporalActivityCommands = new ArrayList<>();

    public StartOfDay(@NonNull String loanId, @NonNull LocalDateTime sodDateTime, TemporalActivityContext sodContext) {
        super(loanId, ACTIVITY_TYPE, getID(sodDateTime));
        this.sodDateTime = sodDateTime;
        this.sodContext = sodContext == null ? new TemporalActivityContext() : sodContext;
        temporalActivityCommands.add(DailyInterestCalculationCommand.class.getSimpleName());
    }

    private static String getID(LocalDateTime sodDateTime) {
        return sodDateTime.toString();
    }

    private String getID() {
        return getID(sodDateTime);
    }

    @Override
    public void applyTo(Ledger ledger) {
        var balance = ledger.getCurrentBalance();
        temporalActivityCommands.forEach(commandName -> {
            var command = TemporalActivityCommandFactory.getCommand(commandName);
            ledger.addEntry(command.execute(ledger.getLoanId(), balance, ACTIVITY_TYPE, getID(), sodDateTime, sodContext));
        });
    }
}
