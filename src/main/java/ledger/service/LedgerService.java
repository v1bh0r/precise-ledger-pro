package ledger.service;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.Ledger;
import ledger.common.LedgerActivity;
import ledger.model.Balance;
import ledger.model.LedgerEntry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class LedgerService {
    @NonNull
    LoanService loanService;

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
    void syncWithRetroactiveLedger(Ledger primaryLedger, Ledger retroactiveLedger, LocalDateTime currentTime,
                                   int forkIndex) {
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
            var activityKey = getActivityKey(retroEntry.getSourceLedgerActivityType(),
                    retroEntry.getSourceLedgerActivityId());
            if (processedActivities.contains(activityKey)) {
                retroactiveIndex++;
                continue;
            }
            var retroTotalImpact = calculateTotalImpact(retroactiveLedger, retroEntry.getSourceLedgerActivityType(),
                    retroEntry.getSourceLedgerActivityId());
            var primaryTotalImpact = calculateTotalImpact(primaryLedger, retroEntry.getSourceLedgerActivityType(),
                    retroEntry.getSourceLedgerActivityId());
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
     * Reverses a ledger activity such as a payment transaction. It is very common to experience such reversals
     * in the financial industry due to various reasons such as customer disputes, low funds, etc.
     */
    public void reverseLedgerActivity(@NonNull String ledgerActivityType, @NonNull String ledgerActivityId,
                                      @NonNull Ledger ledger) {
        // Add a compensation ledger entry to reverse the impact of the activity
        final var compensationEntry = buildCompensationLedgerEntry(ledgerActivityType, ledgerActivityId, ledger);
        ledger.addEntry(compensationEntry);

        // Rollback the ledger to before the reversed activity as a retroactive ledger
        var retroactiveLedger = ledger.rollbackToEntryBefore(ledgerActivityType, ledgerActivityId);
        var forkIndex = retroactiveLedger.getEntries().size(); // Keep note of the fork index to be used later in sync

        // Re-apply all the ledger activities that came after the reversed activity to the retroactive ledger
        var ledgerActivities = loanService.getLedgerActivitiesCreatedSince(ledger.getLoanId(), ledgerActivityType,
                ledgerActivityId);
        applyLedgerActivities(retroactiveLedger, ledgerActivities);

        // Sync the retroactive ledger back into the original ledger
        syncWithRetroactiveLedger(ledger, retroactiveLedger, LocalDateTime.now(), forkIndex);
    }

    /**
     * Builds a compensation ledger entry to reverse the impact of the activity
     */
    private LedgerEntry buildCompensationLedgerEntry(@NotNull String ledgerActivityType,
                                                     @NotNull String ledgerActivityId, @NotNull Ledger ledger) {
        var impactOfReversedActivity = ledger.calculateTotalImpact(ledgerActivityType, ledgerActivityId);
        var negatedImpact = impactOfReversedActivity.negate();
        var newLedgerBalance = ledger.getCurrentBalance().add(negatedImpact);
        return LedgerEntry.builder()
                .loanId(ledger.getLoanId())
                .amount(negatedImpact.getTotalAmount())
                .createdAt(LocalDateTime.now())
                .effectiveAt(LocalDateTime.now())
                .principal(negatedImpact.principal())
                .interest(negatedImpact.interest())
                .fee(negatedImpact.fee())
                .excess(negatedImpact.excess())
                .entryId(String.valueOf(getNextId(ledger.getEntries())))
                .entryType("REVERSAL")
                .principalBalance(newLedgerBalance.principal())
                .interestBalance(newLedgerBalance.interest())
                .feeBalance(newLedgerBalance.fee())
                .excessBalance(newLedgerBalance.excess())
                .sourceLedgerActivityId(ledgerActivityId)
                .sourceLedgerActivityType(ledgerActivityType)
                .build();
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
