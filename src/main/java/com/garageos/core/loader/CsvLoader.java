package com.garageos.core.loader;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvLoader {

    public List<String[]> read(String fileName) {

        List<String[]> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new ClassPathResource(fileName).getInputStream()))) {

            String line;

            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {

                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                // Skip blank lines
                if (line.isBlank()) {
                    continue;
                }

                String[] columns = line.split(",", -1);

                // Trim every column
                for (int i = 0; i < columns.length; i++) {
                    columns[i] = columns[i].trim();
                }

                rows.add(columns);
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return rows;
    }
}