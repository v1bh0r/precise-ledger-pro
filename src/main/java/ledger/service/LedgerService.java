package ledger.service;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.Ledger;
import ledger.common.LedgerActivity;
import ledger.common.ledgeractivity.ReversalActivity;
import ledger.model.Balance;
import ledger.model.LedgerEntry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static ledger.service.LedgerEntryIdService.generateId;

@ApplicationScoped
@RequiredArgsConstructor
public class LedgerService {
    private static final String ADJUSTMENT = "ADJUSTMENT";
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

            var primaryTotalImpact = calculateTotalImpact(primaryLedger, retroEntry.getSourceLedgerActivityType(),
                    retroEntry.getSourceLedgerActivityId());

            if (primaryTotalImpact == null) {
                primaryLedger.addEntry(retroEntry);
                retroactiveIndex++;
                continue;
            } else {

                var retroTotalImpact = calculateTotalImpact(retroactiveLedger,
                        retroEntry.getSourceLedgerActivityType(), retroEntry.getSourceLedgerActivityId());

                var currentPrimaryBalance = primaryLedger.getCurrentBalance();
                if (!retroTotalImpact.equals(primaryTotalImpact)) {
                    var adjustedBalance = retroTotalImpact.subtract(primaryTotalImpact);
                    var adjustment = LedgerEntry.builder().loanId(primaryLedger.getLoanId())
                            .amount(adjustedBalance.getTotalAmount()).createdAt(currentTime).effectiveAt(currentTime)
                            .principal(adjustedBalance.principal()).interest(adjustedBalance.interest())
                            .fee(adjustedBalance.fee()).excess(adjustedBalance.excess()).entryId(generateId())
                            .entryType(ADJUSTMENT)
                            .principalBalance(currentPrimaryBalance.principal().add(adjustedBalance.principal()))
                            .interestBalance(currentPrimaryBalance.interest().add(adjustedBalance.interest()))
                            .feeBalance(currentPrimaryBalance.fee().add(adjustedBalance.fee()))
                            .excessBalance(currentPrimaryBalance.excess().add(adjustedBalance.excess()))
                            .sourceLedgerActivityId(retroEntry.getSourceLedgerActivityId())
                            .sourceLedgerActivityType(retroEntry.getSourceLedgerActivityType()).build();
                    primaryLedger.addEntry(adjustment);
                }
                processedActivities.add(activityKey);
                retroactiveIndex++;
            }
        }
        System.out.println("primaryLedger size:" + primaryLedger.getEntries().size());
    }

    void applyLedgerActivity(Ledger ledger, LedgerActivity ledgerActivity) {
        applyLedgerActivities(ledger, List.of(ledgerActivity));
    }

    void applyLedgerActivities(Ledger ledger, List<LedgerActivity> ledgerActivities) {
        for (var ledgerActivity : ledgerActivities) {
            if (ledgerActivity.isBackdatedEntry(ledger)) {
                // Rollback the ledger to before backdated entry as a retroactive ledger
                var retroactiveLedger = ledger.rollbackToEntryBefore(ledgerActivity.getEffectiveAt());
                var forkIndex = retroactiveLedger.getEntries().size();
                ledgerActivity.applyTo(retroactiveLedger);
                retroactiveLedger.getEntries().subList(forkIndex, retroactiveLedger.getEntries().size())
                        .forEach(ledger::addEntry);
                // Re-apply all the ledger activities that came after the back dated activity to the retroactive ledger
                var ledgerActivitiesEffectiveOnOrAfter =
                        loanService.getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBefore(ledger.getLoanId(),
                                ledgerActivity.getEffectiveAt(), ledgerActivity.getCreatedAt());
                applyLedgerActivities(retroactiveLedger, ledgerActivitiesEffectiveOnOrAfter);

                // Sync the retroactive ledger back into the original ledger
                syncWithRetroactiveLedger(ledger, retroactiveLedger, LocalDateTime.now(), forkIndex);
            } else {
                ledgerActivity.applyTo(ledger);
            }
        }
    }

    Balance calculateTotalImpact(Ledger ledger, String activityType, String activityId) {
        return ledger.calculateTotalImpact(activityType, activityId);
    }

    /**
     * Reverses a ledger activity such as a payment transaction. It is very common to experience such reversals
     * in the financial industry due to various reasons such as customer disputes, low funds, etc.
     */
    public void reverseLedgerActivity(@NonNull ReversalActivity reversalActivity, @NonNull Ledger ledger) {
        String ledgerActivityType = reversalActivity.getReversedActivityType();
        String ledgerActivityId = reversalActivity.getReversedActivityId();

        // Add a compensation ledger entry to reverse the impact of the activity
        final var compensationEntry = buildCompensationLedgerEntry(ledgerActivityType, ledgerActivityId, ledger);
        ledger.addEntry(compensationEntry);

        // Rollback the ledger to before the reversed activity as a retroactive ledger
        var retroactiveLedger = ledger.rollbackToEntryBefore(ledgerActivityType, ledgerActivityId);
        var forkIndex = retroactiveLedger.getEntries().size(); // Keep note of the fork index to be used later in sync

        // Re-apply all the ledger activities that came after the reversed activity to the retroactive ledger
        var ledgerActivities = loanService.getLedgerActivitiesCreatedSinceButBeforeCreatedAt(ledger.getLoanId(),
                ledgerActivityType, ledgerActivityId, reversalActivity.getCreatedAt());
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
        if (impactOfReversedActivity == null) {
            throw new RuntimeException(String.format("Loan %s Attempt to buildCompensationLedgerEntry Ledger " +
                            "Activity: " + " %s Type LedgerActivity Id: %s but the reversed activity has no ledger " +
                            "entries",
                    ledger.getLoanId(), ledgerActivityType, ledgerActivityId));
        }
        var negatedImpact = impactOfReversedActivity.negate();
        var newLedgerBalance = ledger.getCurrentBalance().add(negatedImpact);
        return LedgerEntry.builder().loanId(ledger.getLoanId()).amount(negatedImpact.getTotalAmount())
                .createdAt(LocalDateTime.now()).effectiveAt(LocalDateTime.now()).principal(negatedImpact.principal())
                .interest(negatedImpact.interest()).fee(negatedImpact.fee()).excess(negatedImpact.excess())
                .entryId(generateId()).entryType("REVERSAL").principalBalance(newLedgerBalance.principal())
                .interestBalance(newLedgerBalance.interest()).feeBalance(newLedgerBalance.fee())
                .excessBalance(newLedgerBalance.excess()).sourceLedgerActivityId(ledgerActivityId)
                .sourceLedgerActivityType(ledgerActivityType).build();
    }

    private String getActivityKey(String activityType, String activityId) {
        return activityType + "-" + activityId;
    }
}
