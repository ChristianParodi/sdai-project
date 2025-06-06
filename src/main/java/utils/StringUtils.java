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
}