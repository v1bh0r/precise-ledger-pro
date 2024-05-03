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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@AllArgsConstructor
public class Ledger implements Cloneable {
    private String loanId;
    private Balance startBalance;
    private List<LedgerEntry> entries;
    private String currency;

    public void addEntry(LedgerEntry entry) {
        var newEntry = entry.clone();
        newEntry.updateBalances(this.getCurrentBalance());
        entries.add(newEntry);
    }
    
    public List<LedgerEntry> getEntriesSortedBy(Function<LedgerEntry, LocalDateTime> sorter) {
        return entries.stream().sorted(Comparator.comparing(sorter)).collect(Collectors.toList());
    }

    // TODO: Test that the new ledger is a deep clone of the original ledger
    public Ledger rollbackToEntryBefore(LocalDateTime effectiveAt) {
        // Get entries subset that are effective before the given effectiveAt
        var newEntries = entries.stream().filter(entry -> !entry.getEffectiveAt().isAfter(effectiveAt)).toList();
        return new Ledger(loanId, startBalance, new ArrayList<>(newEntries), currency);
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

    public Balance getBalanceAt(LocalDateTime effectiveAt) {
        // Find the last entry that is effective before the given effectiveAt
        var lastEntryBeforeEffectiveAt = getEntriesSortedBy(LedgerEntry::getEffectiveAt).stream()
                .filter(entry -> !entry.getEffectiveAt().isAfter(effectiveAt)).reduce((first, second) -> second)
                .orElse(null);

        if (lastEntryBeforeEffectiveAt == null) {
            return startBalance;
        } else {
            return lastEntryBeforeEffectiveAt.getBalance();
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

        var matchingEntries = entries.stream().filter(entry -> entry.getSourceLedgerActivityType()
                .equals(activityType) && entry.getSourceLedgerActivityId().equals(activityId)).toList();

        if (matchingEntries.isEmpty()) {
            return null;
        } else {
            matchingEntries.forEach(entry -> {
                totalImpact.set(totalImpact.get().add(entry.getBalanceChange()));
            });
            return totalImpact.get();
        }
    }

}
