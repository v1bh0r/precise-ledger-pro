package ledger.common;

public interface LedgerActivityStrategy {
    void applyTo(Ledger ledger);
}
