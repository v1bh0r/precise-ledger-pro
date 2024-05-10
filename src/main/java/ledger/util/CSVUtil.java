package ledger.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        return parse(reader, type);
    }

    public List<T> parse(Reader reader, Class<T> type) {
        List<T> recordsList = new ArrayList<>();
        try (CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader()
                .withIgnoreHeaderCase().withTrim())) {
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();

            Constructor<T> constructor = type.getConstructor(CSVRecord.class);

            for (CSVRecord csvRecord : csvRecords) {
                T record = constructor.newInstance(csvRecord);
                recordsList.add(record);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recordsList;
    }
}