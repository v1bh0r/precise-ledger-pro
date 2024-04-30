package ledger.service;

import java.util.concurrent.atomic.AtomicInteger;

public class LedgerEntryIdService {
    private static final AtomicInteger identityNumber = new AtomicInteger(100);

    public static String generateId() {
        return String.valueOf(identityNumber.incrementAndGet());
    }
}
