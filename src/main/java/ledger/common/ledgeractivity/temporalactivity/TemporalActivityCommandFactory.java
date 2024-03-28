package ledger.common.ledgeractivity.temporalactivity;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class TemporalActivityCommandFactory {
    private static final Map<String, TemporalActivityCommand> commandMap = new HashMap<>();

    public static void register(TemporalActivityCommand command) {
        commandMap.put(command.getName(), command);
    }

    public static TemporalActivityCommand getCommand(String name) {
        return commandMap.get(name);
    }
}
