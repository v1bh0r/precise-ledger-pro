package ledger.common.ledgeractivity.temporalactivity;

import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A Temporal Activity Context generally contains information necessary to compute the daily interest accrual
 * See the test for an example: ledger.common.ledgeractivity.temporalactivity.StartOfDayTest#applyTo()
 * It can, however, contain different kinds of properties for different kinds of Temporal Activities
 */
@AllArgsConstructor
public class TemporalActivityContext {
    private final Map<String, Object> properties = new HashMap<>();

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public <T> T getProperty(String key, Class<T> type) {
        var obj = properties.get(key);
        if (obj == null) {
            throw new NullPointerException("Property " + key + " is null");
        }
        return type.cast(obj);
    }

    public <T> List<T> getListProperty(String key, Class<T> type) {
        Object obj = properties.get(key);
        if (obj == null) {
            throw new NullPointerException("Property " + key + " is null");
        }
        if (obj instanceof List<?> list) {
            try {
                return list.stream()
                        .map(type::cast)
                        .collect(Collectors.toList());
            } catch (ClassCastException e) {
                throw new ClassCastException("Not all elements in the list are of type " + type.getName());
            }
        } else {
            throw new ClassCastException("Property " + key + " is not a List");
        }
    }
}
