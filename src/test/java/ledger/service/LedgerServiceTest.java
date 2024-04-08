package ledger.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ledger.common.Ledger;
import ledger.model.LedgerEntry;
import ledger.util.CSVUtil;
import ledger.util.ObjectToCsvUtil;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

import static ledger.service.BalanceService.createZeroBalance;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class LedgerServiceTest {
    private static final String BASE_TEST_OUTPUT_DIR = System.getProperty("user.dir") + "/test-output/";
    private ObjectToCsvUtil<LedgerEntry> objectToCsvUtil;
    @Inject
    LedgerService ledgerService;
    @Inject
    Logger log;
    @Inject
    CSVUtil csvUtil;
    private static final String LOAN_ID = "1234";
    private static final String CURRENCY = "USD";
    private static final String DATA_PATH = "data/ledger/service/";

    @BeforeEach
    void setUp() {
        objectToCsvUtil = new ObjectToCsvUtil<>(log);
    }

    @Test
    void deleteme() throws IOException {
        var ledgerJali = initLedger("syncWithRetroactiveLedger_test1/ledger_entries_duplicate.csv");
        objectToCsvUtil.writeListToCsv(ledgerJali.getEntries(), BASE_TEST_OUTPUT_DIR + "syncWithRetroactiveLedger_test1_duplicate.csv");
    }

    @Test
    void syncWithRetroactiveLedger_test1() throws IOException {
        var ledger = initLedger("syncWithRetroactiveLedger_test1/ledger_entries.csv");
        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR + "syncWithRetroactiveLedger_test1_input1.csv");
        var retroactiveLedger = initLedger("syncWithRetroactiveLedger_test1/retroactive_ledger_entries.csv");
        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR + "syncWithRetroactiveLedger_test1_input2.csv");
        var currentTime = LocalDateTime.now();
        ledgerService.syncWithRetroactiveLedger(ledger, retroactiveLedger, currentTime, 4);
        var expectedLedgerAfterSync = initLedger("syncWithRetroactiveLedger_test1/ledger_entries_after_sync.csv");

        objectToCsvUtil.writeListToCsv(ledger.getEntries(), BASE_TEST_OUTPUT_DIR + "syncWithRetroactiveLedger_test1_output.csv");
        assertEquals(expectedLedgerAfterSync.getEntries().size(), ledger.getEntries().size());
        assertEquals(expectedLedgerAfterSync.getCurrentBalance(), ledger.getCurrentBalance());
    }

    private Ledger initLedger(String path) throws IOException {
        var ledgerEntries = csvUtil.parseLedgerEntryCSV(DATA_PATH + path);
        return new Ledger(LOAN_ID, createZeroBalance(CURRENCY), ledgerEntries, CURRENCY);
    }
}