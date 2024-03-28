package ledger.common;

import ledger.model.Balance;
import ledger.model.LedgerEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

@AllArgsConstructor
public class Ledger implements Cloneable {
    @Getter
    private String loanId;
    @Getter
    private Balance startBalance;
    private List<LedgerEntry> entries;
    public void addEntry(LedgerEntry entry) {
        entries.add(entry);
    }

    public List<LedgerEntry> getEntries() {
        return List.copyOf(entries);
    }

    // Test that the new ledger is a deep clone of the original ledger
    public Ledger rollbackTo(LocalDateTime effectiveAt) {
        // Get entries subset that are effective before the given effectiveAt
        var newEntries = entries.stream()
                .filter(entry -> !entry.effectiveAt().isAfter(effectiveAt)).toList();
        return new Ledger(loanId, startBalance, newEntries);
    }

    // Test that the new ledger is a deep clone of the original ledger
    public Ledger rollbackTo(String sourceLedgerActivityType, String sourceLedgerActivityId) {
        var newEntries = new AtomicReference<List<LedgerEntry>>();
        IntStream.range(0, entries.size())
                .filter(i -> entries.get(i).sourceLedgerActivityType().equals(sourceLedgerActivityType) &&
                        entries.get(i).sourceLedgerActivityId().equals(sourceLedgerActivityId))
                .findFirst()
                .ifPresent(i -> newEntries.set(List.copyOf(entries.subList(0, i + 1))));

        return new Ledger(loanId, startBalance, newEntries.get());
    }

    @Override
    public Ledger clone() throws CloneNotSupportedException {
        super.clone();
        return new Ledger(loanId, startBalance, new ArrayList<>(entries));
    }
}
