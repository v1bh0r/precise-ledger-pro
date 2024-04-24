package ledger.common;

import ledger.model.Balance;
import ledger.model.LedgerEntry;
import ledger.service.BalanceService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

@Getter
@AllArgsConstructor
public class Ledger implements Cloneable {
    private String loanId;
    private Balance startBalance;
    private List<LedgerEntry> entries;
    private String currency;

    public void addEntry(LedgerEntry entry) {
        entries.add(entry);
    }

    public List<LedgerEntry> getEntriesSortedByEffectiveAt() {
        var newEntries = new ArrayList<>(entries);
        newEntries.sort(Comparator.comparing(LedgerEntry::getEffectiveAt));
        return newEntries;
    }

    // TODO: Test that the new ledger is a deep clone of the original ledger
    public Ledger rollbackToEntryBefore(LocalDateTime effectiveAt) {
        // Get entries subset that are effective before the given effectiveAt
        var newEntries = entries.stream().filter(entry -> !entry.getEffectiveAt().isAfter(effectiveAt)).toList();
        return new Ledger(loanId, startBalance, newEntries, currency);
    }

    // TODO: Test that the new ledger is a deep clone of the original ledger
    public Ledger rollbackToEntryBefore(String sourceLedgerActivityType, String sourceLedgerActivityId) {
        var newEntries = new AtomicReference<List<LedgerEntry>>();
        IntStream.range(0, entries.size()).filter(i -> entries.get(i).getSourceLedgerActivityType()
                        .equals(sourceLedgerActivityType) && entries.get(i).getSourceLedgerActivityId()
                        .equals(sourceLedgerActivityId)).findFirst()
                .ifPresent(i -> newEntries.set(List.copyOf(entries.subList(0, i))));

        return new Ledger(loanId, startBalance, new ArrayList<>(newEntries.get()), currency);
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

    public Balance calculateTotalImpact(String activityType, String activityId) {
        // Sum up the balances of all entries with the same source activity type and ID
        var entries = this.getEntries();
        AtomicReference<Balance> totalImpact =
                new AtomicReference<>(BalanceService.createZeroBalance(this.getCurrency()));
        entries.stream()
                .filter(entry -> entry.getSourceLedgerActivityType()
                        .equals(activityType) && entry.getSourceLedgerActivityId().equals(activityId))
                .forEach(entry -> {
                    totalImpact.set(totalImpact.get().add(entry.getBalanceChange()));
                });
        return totalImpact.get();
    }
}
