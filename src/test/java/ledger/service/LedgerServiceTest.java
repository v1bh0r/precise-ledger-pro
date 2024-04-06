package ledger.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ledger.common.Ledger;
import ledger.util.CSVUtil;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

import static ledger.service.BalanceService.createZeroBalance;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class LedgerServiceTest {
    @Inject
    LedgerService ledgerService;
    @Inject
    Logger log;
    private static final String LOAN_ID = "10000000";
    private static final String CURRENCY = "USD";
    private static final String DATA_PATH = "data/ledger/service/";

    @Test
    void syncWithRetroactiveLedger_test1() throws IOException {
        var ledger = initLedger("syncWithRetroactiveLedger_test1/ledger_entries.csv");
        var retroactiveLedger = initLedger("syncWithRetroactiveLedger_test1/retroactive_ledger_entries.csv");
        var currentTime = LocalDateTime.now();
        ledgerService.syncWithRetroactiveLedger(ledger, retroactiveLedger, currentTime, 4);
        var expectedLedgerAfterSync = initLedger("syncWithRetroactiveLedger_test1/ledger_entries_after_sync.csv");
        ledger.log(log);
        assertEquals(expectedLedgerAfterSync.getEntries().size(), ledger.getEntries().size());
        assertEquals(expectedLedgerAfterSync.getCurrentBalance(), ledger.getCurrentBalance());
    }

    private Ledger initLedger(String path) throws IOException {
        var ledgerEntries = CSVUtil.parseLedgerEntryCSV(DATA_PATH + path);
        return new Ledger(LOAN_ID, createZeroBalance(CURRENCY), ledgerEntries, CURRENCY);
    }
}