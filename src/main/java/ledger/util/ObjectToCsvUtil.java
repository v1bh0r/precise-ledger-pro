package ledger.util;

import lombok.NoArgsConstructor;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
public class ObjectToCsvUtil<T> {

    public String generateCSV(List<T> objects) throws IllegalAccessException {
        StringBuilder csvContent = new StringBuilder();

        if (objects == null || objects.isEmpty()) {
            return "";
        }

        // Assuming all objects are of the same type, get field names from the first object
        Field[] fields = objects.getFirst().getClass().getDeclaredFields();
        // Filter out all the fields having a name starting with $$_
        fields = Arrays.stream(fields).filter(field -> !field.getName().startsWith("$$")).toArray(Field[]::new);
        for (Field field : fields) {
            csvContent.append(field.getName()).append(",");
        }
        csvContent.deleteCharAt(csvContent.length() - 1); // Remove the last comma
        csvContent.append("\n");

        // Process each object
        for (T obj : objects) {
            for (Field field : fields) {
                field.setAccessible(true); // Make private fields accessible
                Object value = field.get(obj);
                csvContent.append(value).append(",");
            }
            csvContent.deleteCharAt(csvContent.length() - 1); // Remove the last comma
            csvContent.append("\n");
        }
        return csvContent.toString();
    }

    public void writeListToCsv(List<T> objects, String filePath) throws IOException, IllegalAccessException {
        Path pathToFile = Paths.get(filePath);
        Files.createDirectories(pathToFile.getParent()); // Create parent directories if they don't exist
        // Use try-with-resources to ensure FileWriter is closed properly
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(generateCSV(objects));
        }
    }
}
