package ledger.util;

import com.opencsv.bean.CsvToBeanBuilder;
import io.quarkus.arc.log.LoggerName;
import ledger.model.LedgerEntry;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class CSVUtil {
    @LoggerName("CSVUtil")
    Logger log;

    private static BufferedReader readCSV(String resourcePath) throws IOException {
        // Read CSV file
        ClassLoader classLoader = CSVUtil.class.getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource(resourcePath)).getFile());
        System.out.println("Reading CSV file: " + file.getAbsolutePath());
        return Files.newBufferedReader(Paths.get(file.getPath()));

    }

    public static List<LedgerEntry> parseLedgerEntryCSV(String resourcePath) throws IOException {
        var reader = readCSV(resourcePath);
        return new CsvToBeanBuilder<LedgerEntry>(reader)
                .withType(LedgerEntry.class)
                .withIgnoreLeadingWhiteSpace(true)
                .build()
                .parse();
    }
}
