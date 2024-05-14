package ledger.service;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.Ledger;
import ledger.common.LedgerActivity;
import ledger.common.LedgerActivityFactory;
import ledger.common.ledgeractivity.ReversalActivity;
import ledger.common.ledgeractivity.domain.Loan;
import ledger.common.ledgeractivity.temporalactivity.TemporalActivityContext;
import ledger.model.Balance;
import ledger.model.GeneralLedgerActivity;
import ledger.model.LedgerClock;
import ledger.model.LedgerEntry;
import ledger.repository.LedgerActivityRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static ledger.common.MonetaryUtil.toDouble;

@ApplicationScoped
@RequiredArgsConstructor
public class LedgerService {
    private static final String ADJUSTMENT = "Adjustment";
    @NonNull
    LoanService loanService;
    @NonNull
    LedgerActivityFactory ledgerActivityFactory;
    @NonNull
    LedgerActivityRepository ledgerActivityRepository;

    /**
     * Applies ledger activities to the ledger. If a backdated entry is encountered, it creates a retroactive ledger
     * and syncs it back into the original ledger.
     * In the case of a reversal, we do a rollback to the entry before the reversed activity and reapply all the
     * activities that came after the reversed activity to the retroactive ledger.
     * applyLedgerActivities is a recursive function that can call itself multiple times in the case of backdated
     * entries and reversals.
     * The recursion often happens from a distance. So, it might not be immediately obvious.
     * The ledger clock is used to keep track of the current time when applying ledger activities.
     * In the case of the recursion, we often go back and forth in time, so we need to keep track of the current time.
     */
    public void applyLedgerActivities(Ledger ledger, List<LedgerActivity> ledgerActivities,
                                      LedgerClock ledgerClock, TemporalActivityContext temporalActivityContext) {
        for (var ledgerActivity : ledgerActivities) {
            applyLedgerActivity(ledger, ledgerActivity, ledgerClock, temporalActivityContext);
        }
    }

    public LedgerClock getCurrentLedgerClock(Ledger ledger) {
        // TODO: Notice how we are making so many DB hits just to arrive at the activity that caused the last ledger
        //  entries.
        //  The following algorithm is not efficient. We need to optimize it.
        //  The algorithm might not even be correct. We need to write tests to verify our assumptions.
        var entries = ledger.getEntriesSortedBy(LedgerEntry::getCreatedAt);
        if (entries.isEmpty() || entries.getLast() == null) {
            return new LedgerClock(LocalDateTime.MIN);
        } else {
            var lastEntry = entries.getLast();
            var lastActivity = ledgerActivityRepository.findFirstByLoanIdAndTypeAndId(ledger.getLoanId(),
                    lastEntry.getSourceLedgerActivityType(), lastEntry.getSourceLedgerActivityId());
            if (lastActivity == null) {
                throw new RuntimeException("LedgerActivity not found for LoanId: " + ledger.getLoanId() + " Type: " +
                        lastEntry.getSourceLedgerActivityType() + " Id: " + lastEntry.getSourceLedgerActivityId());
            }
            return new LedgerClock(lastActivity.getTransactionTime());
        }
    }

    public void applyLedgerActivity(Ledger ledger, LedgerActivity ledgerActivity, LedgerClock ledgerClock,
                                    TemporalActivityContext temporalActivityContext) {
        if (ledgerActivity.isBackdatedEntry(ledger)) {
            performBackdatedActivity(ledger, ledgerActivity, ledgerClock, temporalActivityContext);
        } else {
            ledgerActivity.applyTo(ledger, ledgerClock, temporalActivityContext);
        }
    }

    /**
     * Reconciles the ledger with another ledger containing
     * historical transactions. It does so by adding
     * the entries from the retroactiveLedger to this ledger.
     * It makes adjustments for any differences in the
     * entries between the two ledgers grouped by the source
     * event type and ID.
     *
     * @param primaryLedger         The ledger from which a retroactive ledger was forked from
     * @param retroactiveLedger     The ledger having historical transactions that need to be reconciled
     * @param currentTime           The current system time injected from outside-in for testability
     * @param adjustmentEffectiveAt The effective date of the adjustment entries that might be created from the sync
     * @param forkIndex             The index at which the retroactive ledger was forked from the primary ledger
     */
    public void syncWithRetroactiveLedger(Ledger primaryLedger, Ledger retroactiveLedger, LocalDateTime currentTime,
                                          LocalDateTime adjustmentEffectiveAt, int forkIndex) {
        List<LedgerEntry> retroactiveLedgerEntries = retroactiveLedger.getEntriesSortedBy(LedgerEntry::getEffectiveAt);
        var primaryLedgerEntries = primaryLedger.getEntriesSortedBy(LedgerEntry::getEffectiveAt);
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
            } else {

                var retroTotalImpact = calculateTotalImpact(retroactiveLedger,
                        retroEntry.getSourceLedgerActivityType(), retroEntry.getSourceLedgerActivityId());

                var currentPrimaryBalance = primaryLedger.getCurrentBalance();
                if (!retroTotalImpact.equals(primaryTotalImpact)) {
                    var adjustedBalance = retroTotalImpact.subtract(primaryTotalImpact);
                    var adjustment = LedgerEntry.builder().loanId(primaryLedger.getLoanId())
                            .amount(toDouble(adjustedBalance.getTotalAmount())).createdAt(currentTime)
                            .effectiveAt(adjustmentEffectiveAt).principal(toDouble(adjustedBalance.principal()))
                            .interest(toDouble(adjustedBalance.interest())).fee(toDouble(adjustedBalance.fee()))
                            .excess(toDouble(adjustedBalance.excess())).entryType(ADJUSTMENT)
                            .principalBalance(toDouble(currentPrimaryBalance.principal()
                                    .add(adjustedBalance.principal())))
                            .interestBalance(toDouble(currentPrimaryBalance.interest().add(adjustedBalance.interest())))
                            .feeBalance(toDouble(currentPrimaryBalance.fee().add(adjustedBalance.fee())))
                            .excessBalance(toDouble(currentPrimaryBalance.excess().add(adjustedBalance.excess())))
                            .sourceLedgerActivityId(retroEntry.getSourceLedgerActivityId())
                            .sourceLedgerActivityType(retroEntry.getSourceLedgerActivityType()).build();
                    primaryLedger.addEntry(adjustment);
                }
                processedActivities.add(activityKey);
            }
            retroactiveIndex++;
        }
        System.out.println("primaryLedger size:" + primaryLedger.getEntries().size());
    }

    private void performBackdatedActivity(Ledger ledger, LedgerActivity ledgerActivity,
                                          LedgerClock ledgerClock, TemporalActivityContext temporalActivityContext) {
        // Rollback the ledger to before backdated entry as a retroactive ledger
        var retroactiveLedger = ledger.rollbackToEntryBefore(ledgerActivity.getEffectiveAt());
        var forkIndex = retroactiveLedger.getEntries().size();
        ledgerActivity.applyTo(retroactiveLedger, ledgerClock, temporalActivityContext);
        retroactiveLedger.getEntries().subList(forkIndex, retroactiveLedger.getEntries().size())
                .forEach(ledger::addEntry);
        // Re-apply all the ledger activities that came after the back dated activity to the retroactive ledger
        var ledgerActivitiesEffectiveOnOrAfter =
                loanService.getLedgerActivitiesEffectiveOnOrAfterAndCreatedOnOrBefore(ledger.getLoanId(),
                                ledgerActivity.getEffectiveAt(), ledgerClock.getNow())
                        // Sort by effectiveAt because at this point in time, we can end in an infinite loop if the
                        // activities keep
                        // getting processed as back-dated entries
                        .stream().sorted(Comparator.comparing(GeneralLedgerActivity::getEffectiveAt)).toList();
        applyLedgerActivities(retroactiveLedger, ledgerActivitiesEffectiveOnOrAfter.stream()
                .map(generalActivity -> ledgerActivityFactory.create(generalActivity))
                .toList(), ledgerClock, temporalActivityContext);

        // Sync the retroactive ledger back into the original ledger
        syncWithRetroactiveLedger(ledger, retroactiveLedger, ledgerClock.getNow(), ledgerActivity.getCreatedAt(),
                forkIndex);
    }

    Balance calculateTotalImpact(Ledger ledger, String activityType, String activityId) {
        return ledger.calculateTotalImpact(activityType, activityId);
    }

    /**
     * Reverses a ledger activity such as a payment transaction. It is very common to experience such reversals
     * in the financial industry due to various reasons such as customer disputes, low funds, etc.
     */
    public void reverseLedgerActivity(@NonNull ReversalActivity reversalActivity, @NonNull Ledger ledger,
                                      @NotNull LedgerClock ledgerClock,
                                      @NonNull TemporalActivityContext temporalActivityContext) {
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
                ledgerActivityType, ledgerActivityId, ledgerClock.getNow());
        // Filter out the reversal activity itself if encountered to avoid infinite recursion
        var stream = ledgerActivities.stream().filter(activity -> !activity.equals(reversalActivity));
        // Sort by effectiveAt because at this point in time, we can end in an infinite loop if the activities keep
        // getting processed as back-dated entries
        ledgerActivities = stream.sorted(Comparator.comparing(GeneralLedgerActivity::getEffectiveAt)).toList();
        applyLedgerActivities(retroactiveLedger, ledgerActivities.stream()
                .map(ledgerActivityFactory::create).toList(), ledgerClock, temporalActivityContext);

        // Sync the retroactive ledger back into the original ledger
        syncWithRetroactiveLedger(ledger, retroactiveLedger, LocalDateTime.now(), reversalActivity.getCreatedAt(),
                forkIndex);
    }

    /**
     * Builds a compensation ledger entry to reverse the impact of the activity
     */
    private LedgerEntry buildCompensationLedgerEntry(@NotNull String ledgerActivityType,
                                                     @NotNull String ledgerActivityId, @NotNull Ledger ledger) {
        if (ledgerActivityType.isBlank() || ledgerActivityId.isBlank()) {
            throw new IllegalArgumentException("LedgerActivityType and LedgerActivityId cannot be blank");
        }

        var impactOfReversedActivity = ledger.calculateTotalImpact(ledgerActivityType, ledgerActivityId);
        if (impactOfReversedActivity == null) {
            throw new RuntimeException(String.format("Loan %s Attempt to buildCompensationLedgerEntry Ledger " +
                    "Activity: " + " %s Type LedgerActivity Id: %s but the reversed activity has no ledger " +
                    "entries", ledger.getLoanId(), ledgerActivityType, ledgerActivityId));
        }

        var negatedImpact = impactOfReversedActivity.negate();
        var newLedgerBalance = ledger.getCurrentBalance().add(negatedImpact);
        return LedgerEntry.builder().loanId(ledger.getLoanId()).amount(toDouble(negatedImpact.getTotalAmount()))
                .createdAt(LocalDateTime.now()).effectiveAt(LocalDateTime.now())
                .principal(toDouble(negatedImpact.principal()))
                .interest(toDouble(negatedImpact.interest())).fee(toDouble(negatedImpact.fee()))
                .excess(toDouble(negatedImpact.excess()))
                .entryType("Reversal").principalBalance(toDouble(newLedgerBalance.principal()))
                .interestBalance(toDouble(newLedgerBalance.interest())).feeBalance(toDouble(newLedgerBalance.fee()))
                .excessBalance(toDouble(newLedgerBalance.excess())).sourceLedgerActivityId(ledgerActivityId)
                .sourceLedgerActivityType(ledgerActivityType).build();
    }

    private String getActivityKey(String activityType, String activityId) {
        return activityType + "-" + activityId;
    }

    public Ledger getLedger(UUID loanId, LocalDateTime since) {
        Loan loan = Loan.findById(loanId);

        List<LedgerEntry> ledgerEntries = LedgerEntry.find("loanId = ?1 and createdAt > ?2", loanId.toString(),
                        since)
                .list();
        return new Ledger(loanId.toString(), loan.getStartingLedgerBalance(), ledgerEntries, loan.getCurrencyCode());
    }

    public Ledger getLedger(UUID loanId) {
        Loan loan = Loan.findById(loanId);

        List<LedgerEntry> ledgerEntries = LedgerEntry.find("loanId = ?1", loanId.toString())
                .list();
        return new Ledger(loanId.toString(), loan.getStartingLedgerBalance(), ledgerEntries, loan.getCurrencyCode());
    }
}
