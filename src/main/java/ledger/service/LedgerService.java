package ledger.service;

import ledger.model.Balance;
import ledger.common.Ledger;
import ledger.common.LedgerActivity;

import java.util.List;

public class LedgerService {
    /**
     * Reconciles the ledger with another ledger containing
     * historical transactions. It does so by adding
     * the entries from the retroactiveLedger to this ledger.
     * It makes adjustments for any differences in the
     * entries between the two ledgers grouped by the source
     * event type and ID.
     *
     * @param primaryLedger The ledger from which a retroactive ledger was forked from
     * @param retroactiveLedger The ledger having historical transactions that need to be reconciled
     */
    void syncWithRetroactiveLedger(Ledger primaryLedger, Ledger retroactiveLedger) {

    }

    void applyLedgerActivities(Ledger ledger, List<LedgerActivity> ledgerActivities) {
        for (var ledgerActivity : ledgerActivities) {
            ledgerActivity.applyTo(ledger);
        }
    }

    Balance calculateTotalImpact(LedgerActivity ledgerActivity) {
        return null;
    }
}
