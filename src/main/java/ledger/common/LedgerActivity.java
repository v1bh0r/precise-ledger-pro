package ledger.common;

import ledger.model.LedgerClock;
import ledger.model.LedgerEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public abstract class LedgerActivity {
    @NonNull
    String loanId;
    @NonNull
    String commonName;
    @NonNull
    String activityType;
    @NonNull
    String activityId;
    @NonNull
    LocalDateTime effectiveAt;
    @NonNull
    LocalDateTime createdAt;

    public String toString() {
        return String.format("ActivityType: %s ActivityId: %s EffectiveAt: %s CreatedAt: %s", activityType,
                activityId, effectiveAt, createdAt);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LedgerActivity other)) {
            return false;
        }
        return activityType.equals(other.activityType) && activityId.equals(other.activityId);
    }

    public void applyTo(Ledger ledger, LedgerClock ledgerClock) {
        if (isBackdatedEntry(ledger)) {
            /*
             * We cannot add backdated entries to the ledger from here.
             * In order to handle backdated entries, we need to create a new retroactive ledger by rolling back the
             * ledger to the effectiveAt date of the backdated entry and then reapplying all the ledger activities.
             * Then you sync the retroactive ledger back into the original ledger.
             * See ARCHITECTURE.md for more details.
             */
            throw new IllegalArgumentException(String.format("LoanId: %s ActivityType: %s ActivityId: %s ; Cannot " + "add" + " backdated entries to the ledger", loanId, activityType, activityId));

        }
        generateLedgerEntries(ledger, ledgerClock);
        ledgerClock.advanceTime(this.createdAt);
    }

    public boolean isBackdatedEntry(Ledger ledger) {
        var entries = ledger.getEntriesSortedBy(LedgerEntry::getEffectiveAt);
        if (entries.isEmpty()) {
            return false;
        } else {
            return effectiveAt.isBefore(entries.getLast().getEffectiveAt());
        }
    }

    /**
     * Adds one or more Ledger Activities to the ledger depending on the type of Ledger Activity
     *
     * @param ledger      the ledger to which the Ledger Activities are added
     * @param ledgerClock the clock that keeps track of the current time
     */
    protected abstract void generateLedgerEntries(Ledger ledger,
                                                  LedgerClock ledgerClock);
}
