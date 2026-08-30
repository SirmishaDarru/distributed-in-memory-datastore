package com.datastore.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses Redis Serialization Protocol (RESP) array requests from a client stream.
 *
 * <p>Expected shape for a command:
 * <pre>
 * *2\r\n
 * $4\r\n
 * ECHO\r\n
 * $5\r\n
 * hello\r\n
 * </pre>
 */
public class RespParser {

    /**
     * Reads one RESP array of bulk strings from {@code reader}.
     *
     * @return the parsed elements, or an empty list if the stream is closed or the
     *         frame is incomplete / malformed
     */
    public List<String> parse(BufferedReader reader) throws IOException {
        if (reader == null) {
            return Collections.emptyList();
        }

        String header = reader.readLine();
        if (header == null || header.isEmpty() || !header.startsWith("*")) {
            return Collections.emptyList();
        }

        int elementCount;
        try {
            elementCount = Integer.parseInt(header.substring(1));
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }

        if (elementCount < 0) {
            return Collections.emptyList();
        }
        if (elementCount == 0) {
            return new ArrayList<>();
        }

        List<String> elements = new ArrayList<>(elementCount);
        for (int i = 0; i < elementCount; i++) {
            String bulkHeader = reader.readLine();
            if (bulkHeader == null || !bulkHeader.startsWith("$")) {
                return Collections.emptyList();
            }

            int length;
            try {
                length = Integer.parseInt(bulkHeader.substring(1));
            } catch (NumberFormatException e) {
                return Collections.emptyList();
            }

            if (length < 0) {
                elements.add(null);
                continue;
            }

            String data = reader.readLine();
            if (data == null) {
                return Collections.emptyList();
            }
            elements.add(data);
        }

        return elements;
    }
}
