package ledger.util;

import com.opencsv.bean.CsvToBeanBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class CSVUtil<T> {
    private BufferedReader readCSV(String resourcePath, Class<T> clazz) throws IOException {
        // Read CSV file
        ClassLoader classLoader = clazz.getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource(resourcePath)).getFile());
        System.out.println("Reading CSV file: " + file.getAbsolutePath());
        return Files.newBufferedReader(Paths.get(file.getPath()));

    }

    public List<T> parse(String resourcePath, Class<T> type) throws IOException {
        var reader = readCSV(resourcePath, type);
        return new CsvToBeanBuilder<T>(reader)
                // The class of T
                .withType(type)
                .withIgnoreLeadingWhiteSpace(true)
                .build()
                .parse();
    }
}
