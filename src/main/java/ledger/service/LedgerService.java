package ledger.service;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.Ledger;
import ledger.common.LedgerActivity;
import ledger.model.Balance;
import ledger.model.LedgerEntry;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@ApplicationScoped
public class LedgerService {
    /**
     * Reconciles the ledger with another ledger containing
     * historical transactions. It does so by adding
     * the entries from the retroactiveLedger to this ledger.
     * It makes adjustments for any differences in the
     * entries between the two ledgers grouped by the source
     * event type and ID.
     *
     * @param primaryLedger     The ledger from which a retroactive ledger was forked from
     * @param retroactiveLedger The ledger having historical transactions that need to be reconciled
     * @param currentTime       The current system time injected from outside-in for testability
     * @param forkIndex         The index at which the retroactive ledger was forked from the primary ledger
     */
    void syncWithRetroactiveLedger(Ledger primaryLedger, Ledger retroactiveLedger, LocalDateTime currentTime, int forkIndex) {
        List<LedgerEntry> retroactiveLedgerEntries = retroactiveLedger.getEntriesSortedByEffectiveAt();
        var primaryLedgerEntries = primaryLedger.getEntriesSortedByEffectiveAt();
        if (retroactiveLedgerEntries.isEmpty() || primaryLedgerEntries.isEmpty()) {
            return;
        }
        var processedActivities = new HashSet<String>();
        var nextId = getNextId(primaryLedgerEntries);
        var retroactiveIndex = forkIndex;

        // Apply the retroactive ledger entries to the primary ledger
        while (retroactiveIndex < retroactiveLedgerEntries.size()) {
            var retroEntry = retroactiveLedgerEntries.get(retroactiveIndex);
            var activityKey = getActivityKey(retroEntry.getSourceLedgerActivityType(), retroEntry.getSourceLedgerActivityId());
            if (processedActivities.contains(activityKey)) {
                retroactiveIndex++;
                continue;
            }
            var retroTotalImpact = calculateTotalImpact(retroactiveLedger, retroEntry.getSourceLedgerActivityType(), retroEntry.getSourceLedgerActivityId());
            var primaryTotalImpact = calculateTotalImpact(primaryLedger, retroEntry.getSourceLedgerActivityType(), retroEntry.getSourceLedgerActivityId());
            var currentPrimaryBalance = primaryLedger.getCurrentBalance();
            if (!retroTotalImpact.equals(primaryTotalImpact)) {
                var adjustedBalance = retroTotalImpact.subtract(primaryTotalImpact);
                var adjustment = LedgerEntry.builder()
                        .loanId(primaryLedger.getLoanId())
                        .amount(adjustedBalance.getTotalAmount())
                        .createdAt(currentTime)
                        .effectiveAt(currentTime)
                        .principal(adjustedBalance.principal())
                        .interest(adjustedBalance.interest())
                        .fee(adjustedBalance.fee())
                        .excess(adjustedBalance.excess())
                        .entryId(String.valueOf(nextId))
                        .entryType("ADJUSTMENT")
                        .principalBalance(currentPrimaryBalance.principal().add(adjustedBalance.principal()))
                        .interestBalance(currentPrimaryBalance.interest().add(adjustedBalance.interest()))
                        .feeBalance(currentPrimaryBalance.fee().add(adjustedBalance.fee()))
                        .excessBalance(currentPrimaryBalance.excess().add(adjustedBalance.excess()))
                        .sourceLedgerActivityId(retroEntry.getSourceLedgerActivityId())
                        .sourceLedgerActivityType(retroEntry.getSourceLedgerActivityType())
                        .build();
                primaryLedger.addEntry(adjustment);
                nextId++;
            }
            processedActivities.add(activityKey);
            retroactiveIndex++;
        }
        System.out.println("primaryLedger size:" + primaryLedger.getEntries().size());
    }

    void applyLedgerActivities(Ledger ledger, List<LedgerActivity> ledgerActivities) {
        for (var ledgerActivity : ledgerActivities) {
            ledgerActivity.applyTo(ledger);
        }
    }

    Balance calculateTotalImpact(Ledger ledger, String activityType, String activityId) {
        return ledger.calculateTotalImpact(activityType, activityId);
    }

    /**
     * 1. Rollback the ledger to before the reversed activity as a retroactive ledger
     * 2. Re-apply all the ledger activities that came after the reversed activity
     * to the retroactive ledger
     * 3. Add a compensation ledger entry as a reversal entry to the original ledger
     * 4. Sync the retroactive ledger back into the original ledger
     */
    public void reverseLedgerActivity(@NonNull String ledgerActivityType, @NonNull String ledgerActivityId, @NonNull Ledger ledger) {
        // TODO: Implement this method
    }

    private String getActivityKey(String activityType, String activityId) {
        return activityType + "-" + activityId;
    }

    // TODO: We need a better way to generate IDs in order to make it thread-safe and dist prog compatible
    public static int getNextId(List<LedgerEntry> entries) {
        // Find the greatest ID in the list and increment it by 1
        return entries.stream()
                .map(LedgerEntry::getEntryId)
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0) + 1;
    }
}
