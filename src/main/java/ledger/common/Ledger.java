package ledger.common;

import ledger.model.Balance;
import ledger.model.LedgerEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

@AllArgsConstructor
public class Ledger implements Cloneable {
    @Getter
    private String loanId;
    @Getter
    private Balance startBalance;
    @Getter
    private List<LedgerEntry> entries;
    @Getter
    private String currency;

    public void addEntry(LedgerEntry entry) {
        entries.add(entry);
    }

    public List<LedgerEntry> getEntriesSortedByEffectiveAt() {
        entries.sort(Comparator.comparing(LedgerEntry::getEffectiveAt));
        return entries;
    }

    // Test that the new ledger is a deep clone of the original ledger
    public Ledger rollbackTo(LocalDateTime effectiveAt) {
        // Get entries subset that are effective before the given effectiveAt
        var newEntries = entries.stream().filter(entry -> !entry.getEffectiveAt().isAfter(effectiveAt)).toList();
        return new Ledger(loanId, startBalance, newEntries, currency);
    }

    // Test that the new ledger is a deep clone of the original ledger
    public Ledger rollbackTo(String sourceLedgerActivityType, String sourceLedgerActivityId) {
        var newEntries = new AtomicReference<List<LedgerEntry>>();
        IntStream.range(0, entries.size()).filter(i -> entries.get(i).getSourceLedgerActivityType().equals(sourceLedgerActivityType) && entries.get(i).getSourceLedgerActivityId().equals(sourceLedgerActivityId)).findFirst().ifPresent(i -> newEntries.set(List.copyOf(entries.subList(0, i + 1))));

        return new Ledger(loanId, startBalance, newEntries.get(), currency);
    }

    @Override
    public Ledger clone() throws CloneNotSupportedException {
        super.clone();
        return new Ledger(loanId, startBalance, new ArrayList<>(entries), currency);
    }

    public Balance getCurrentBalance() {
        if (entries.isEmpty()) {
            return startBalance;
        } else {
            return entries.getLast().getBalance();
        }
    }

    public void log(Logger log) {
        log.info("Ledger for loanId: " + loanId);
        entries.forEach(entry -> log.info(entry.toString()));
    }
}
