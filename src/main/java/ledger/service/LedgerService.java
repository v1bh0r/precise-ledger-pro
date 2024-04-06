package ledger.service;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.Ledger;
import ledger.common.LedgerActivity;
import ledger.model.Balance;
import ledger.model.LedgerEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
     */
    void syncWithRetroactiveLedger(Ledger primaryLedger, Ledger retroactiveLedger, LocalDateTime currentTime, int forkIndex) {
        List<LedgerEntry> retroactiveLedgerEntries = retroactiveLedger.getEntriesSortedByEffectiveAt();
        var primaryLedgerEntries = primaryLedger.getEntriesSortedByEffectiveAt();        //12-13
        if (retroactiveLedgerEntries.isEmpty() || primaryLedgerEntries.isEmpty()) {
            return;
        }

        //primaryLedger of size 4
        var retroactiveIndex = forkIndex; // 4

        // Apply the retroactive ledger entries to the primary ledger
        while (retroactiveIndex < retroactiveLedgerEntries.size()) {
            var retroEntry = retroactiveLedgerEntries.get(retroactiveIndex);//4 to 12
            var retroTotalImpact = calculateTotalImpact(retroactiveLedger, retroEntry.getSourceLedgerActivityType(), retroEntry.getSourceLedgerActivityId());
            var primaryTotalImpact = calculateTotalImpact(primaryLedger, retroEntry.getSourceLedgerActivityType(), retroEntry.getSourceLedgerActivityId());
            var currentPrimaryBalance = primaryLedger.getCurrentBalance();
            if (!retroTotalImpact.equals(primaryTotalImpact)) {
                var adjustedBalance = retroTotalImpact.subtract(primaryTotalImpact);
                var adjustment = LedgerEntry.builder()
                        .amount(adjustedBalance.getTotalAmount())
                        .createdAt(currentTime)
                        .effectiveAt(currentTime)
                        .principal(adjustedBalance.principal())
                        .interest(adjustedBalance.interest())
                        .fee(adjustedBalance.fee())
                        .excess(adjustedBalance.excess())
                        .entryId("ADJ-" + currentTime)
                        .entryType("ADJUSTMENT")
                        .principalBalance(currentPrimaryBalance.principal().add(adjustedBalance.principal()))
                        .interestBalance(currentPrimaryBalance.interest().add(adjustedBalance.interest()))
                        .feeBalance(currentPrimaryBalance.fee().add(adjustedBalance.fee()))
                        .excessBalance(currentPrimaryBalance.excess().add(adjustedBalance.excess()))
                        .sourceLedgerActivityId(retroEntry.getSourceLedgerActivityId())
                        .sourceLedgerActivityType(retroEntry.getSourceLedgerActivityType())
                        .build();
                primaryLedger.addEntry(adjustment);
            }
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
        // Sum up the balances of all entries with the same source activity type and ID
        var entries = ledger.getEntries();
        AtomicReference<Balance> totalImpact = new AtomicReference<>(BalanceService.createZeroBalance(ledger.getCurrency()));
        entries.stream()
                .filter(entry -> entry.getSourceLedgerActivityType().equals(activityType) && entry.getSourceLedgerActivityId().equals(activityId))
                .forEach(entry -> {
                    totalImpact.set(totalImpact.get().add(entry.getBalanceChange()));
                });
        return totalImpact.get();
    }
}
