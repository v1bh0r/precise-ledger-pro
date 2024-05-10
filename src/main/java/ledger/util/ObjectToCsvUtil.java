package ledger.util;

import lombok.AllArgsConstructor;
import org.jboss.logging.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@AllArgsConstructor
public class ObjectToCsvUtil<T> {

    private Logger log;

    public void writeListToCsv(List<T> objects, String filePath) {
        StringBuilder csvContent = new StringBuilder();

        if (objects == null || objects.isEmpty()) {
            return;
        }

        // Assuming all objects are of the same type, get field names from the first object
        Field[] fields = objects.get(0).getClass().getDeclaredFields();
        for (Field field : fields) {
            csvContent.append(field.getName()).append(",");
        }
        csvContent.deleteCharAt(csvContent.length() - 1); // Remove the last comma
        csvContent.append("\n");

        // Process each object
        for (T obj : objects) {
            for (Field field : fields) {
                try {
                    field.setAccessible(true); // Make private fields accessible
                    Object value = field.get(obj);
                    csvContent.append(value).append(",");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            csvContent.deleteCharAt(csvContent.length() - 1); // Remove the last comma
            csvContent.append("\n");
        }

        try {
            Path pathToFile = Paths.get(filePath);
            Files.createDirectories(pathToFile.getParent()); // Create parent directories if they don't exist

            // Use try-with-resources to ensure FileWriter is closed properly
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(csvContent.toString());
            }
        } catch (IOException e) {
            log.error(e);
        }
    }
}
