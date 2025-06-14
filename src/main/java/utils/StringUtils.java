package utils;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    public static String unescape(String raw) {
        if (raw == null)
            return null;

        String result = raw.replace("\\n", "\n");
        result = result.replace("\\t", "\t");
        result = result.replace("\\r", "\r");
        result = result.replace("\"\"", "\""); // Unescape double quotes
        return result;
    }

    public static String wrapText(String text, int maxCols) {
        if (text == null) {
            return null;
        }

        String[] paragraphs = text.split("\\r?\\n", -1);
        List<String> wrappedParagraphs = new ArrayList<>();

        for (String para : paragraphs) {
            if (para.isEmpty()) {
                wrappedParagraphs.add("");
                continue;
            }

            String[] words = para.split("\\s+");
            StringBuilder currentLine = new StringBuilder();
            StringBuilder wrappedPara = new StringBuilder();

            for (String word : words) {
                if (currentLine.length() == 0) {
                    currentLine.append(word);
                } else if (currentLine.length() + 1 + word.length() <= maxCols) {
                    currentLine.append(' ').append(word);
                } else {
                    wrappedPara.append(currentLine).append('\n');
                    currentLine.setLength(0);
                    currentLine.append(word);
                }
            }
            if (currentLine.length() > 0) {
                wrappedPara.append(currentLine);
            }

            wrappedParagraphs.add(wrappedPara.toString());
        }

        return String.join("\n", wrappedParagraphs);
    }

    public static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString().trim());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }
        result.add(currentField.toString().trim());
        return result.toArray(new String[0]);
    }
}