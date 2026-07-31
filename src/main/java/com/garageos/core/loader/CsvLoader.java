package com.garageos.core.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CsvLoader {

    private static final int BUFFER_SIZE = 64 * 1024;

    public void read(
            String fileName,
            CsvRowHandler handler) {

        ClassPathResource resource =
                new ClassPathResource(fileName);

        if (!resource.exists()) {

            log.info("CSV [{}] not found. Skipping import.", fileName);

            return;
        }

        try (BufferedReader reader = createReader(resource)) {

            String headerLine = reader.readLine();

            if (headerLine == null) {

                log.warn("CSV [{}] is empty.", fileName);

                return;
            }

            Map<String, Integer> headers =
                    buildHeaders(headerLine);

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.isBlank()) {
                    continue;
                }

                CsvRow row =
                        new CsvRow(
                                split(line),
                                headers);

                handler.handle(row);
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to read csv : " + fileName,
                    e);

        }

    }

    private BufferedReader createReader(
            ClassPathResource resource) throws IOException {

        return new BufferedReader(

                new InputStreamReader(

                        resource.getInputStream(),

                        StandardCharsets.UTF_8

                ),

                BUFFER_SIZE

        );

    }

    private Map<String, Integer> buildHeaders(
            String headerLine) {

        String[] columns =
                split(headerLine);

        Map<String, Integer> headers =
                new HashMap<>(columns.length);

        for (int i = 0; i < columns.length; i++) {

            headers.put(
                    columns[i].trim(),
                    i);

        }

        return headers;

    }


    private String[] split(String line) {

        List<String> values =
                new ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {

            char c = line.charAt(i);

            if (c == '"') {

                inQuotes = !inQuotes;

            } else if (c == ',' && !inQuotes) {

                values.add(
                        current.toString().trim());

                current.setLength(0);

            } else {

                current.append(c);

            }

        }

        values.add(
                current.toString().trim());

        return values.toArray(new String[0]);

    }

}