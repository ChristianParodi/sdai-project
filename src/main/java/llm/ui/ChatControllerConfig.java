package llm.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import utils.StringUtils;

public class ChatControllerConfig {
  public static final String DEFAULT_SYSTEM_PROMPT = "You are a NetLogo coding assistant. Respond ONLY with the following format and nothing else:\n\n"
      +
      "CODE:\n```netlogo\n[NetLogo code here]\n```\n\n" +
      "Do NOT add greetings, sign-offs, extra commentary, or offer further help. " +
      "Do NOT include anything outside the CODE section. " +
      "Do NOT copy or cite the examples below. Generate a new answer for the user request. " +
      "Here are some examples to guide you (do not copy them):\n\n" + loadNetLogoExamples();

  private static String loadNetLogoExamples() {
    StringBuilder examples = new StringBuilder();
    try (InputStream is = ChatControllerConfig.class.getResourceAsStream("/netlogo_fine_tune.csv");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      String line;
      boolean isFirstLine = true;
      while ((line = reader.readLine()) != null) {
        if (isFirstLine) {
          isFirstLine = false;
          continue; // Skip header
        }
        // Parse CSV line using StringUtils
        String[] parts = StringUtils.parseCsvLine(line);
        if (parts.length >= 3) {
          String exampleNum = parts[0];
          String code = StringUtils.unescape(parts[1]);
          String annotation = StringUtils.unescape(parts[2]);
          examples.append("Example ").append(exampleNum).append(":\n");
          examples.append("CODE:\n```nlogo\n").append(code).append("\n```\n");
          examples.append("EXPLANATION:\n").append(annotation).append("\n\n");
        }
      }
    } catch (IOException e) {
      System.err.println("Error loading NetLogo examples: " + e.getMessage());
      // Return a few basic examples if file loading fails
      return "Example: Create 10 turtles\nCODE:\n```netlogo\ncrt 10\n```\n\n";
    }
    System.out.println("DEBUG: Loaded NetLogo examples:\n" + examples);
    return examples.toString();
  }
}
