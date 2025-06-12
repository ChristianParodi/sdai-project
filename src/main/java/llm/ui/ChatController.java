package llm.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.JsonObject;
import ollama.OllamaClient;
import org.nlogo.window.GUIWorkspace;

import javax.swing.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the chat pane lifecycle and orchestrates LLM calls via OllamaClient.
 */
public class ChatController {
    private final ChatPane pane;
    private final JFrame frame;
    private final List<JsonObject> messages = new ArrayList<>();
    private final GUIWorkspace workspace;
    private final List<String> recentNetLogoCode = new ArrayList<>(); // Track last 5 code parts
    private static final int MAX_CODE_HISTORY = 5;

    static {
        // Apply FlatLaf theme
        System.setProperty("flatlaf.uiScale", "1.0");
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("FlatLaf init failed: " + e.getMessage());
        }
    }

    private String buildSimplePrompt(String userQuery) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(
                "You are a NetLogo coding assistant. When asked to generate NetLogo code, respond in this exact format:\n\n");
        prompt.append("CODE:\n```nlogo\n[your NetLogo code here]\n```\n\n");
        prompt.append("EXPLANATION:\n[brief explanation of the generated code]\n\n");

        // Add few-shot examples from CSV
        prompt.append("Here are some examples to guide you:\n\n");
        prompt.append(loadNetLogoExamples());

        prompt.append("User request: ").append(userQuery);

        return prompt.toString();
    }

    private String loadNetLogoExamples() {
        StringBuilder examples = new StringBuilder();

        try (InputStream is = getClass().getResourceAsStream("/netlogo_fine_tune.csv");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }

                // Parse CSV line (simple parsing, assumes no commas in quoted strings)
                String[] parts = parseCsvLine(line);
                if (parts.length >= 3) {
                    String exampleNum = parts[0];
                    String code = parts[1].replace("\"\"", "\""); // Unescape double quotes
                    String annotation = parts[2].replace("\"\"", "\""); // Unescape double quotes

                    examples.append("Example ").append(exampleNum).append(":\n");
                    examples.append("Request: ").append(annotation).append("\n");
                    examples.append("CODE:\n```nlogo\n").append(code).append("\n```\n\n");
                }
            }

        } catch (IOException e) {
            System.err.println("Error loading NetLogo examples: " + e.getMessage());
            // Return a few basic examples if file loading fails
            return "Example: Create 10 turtles\nCODE:\n```nlogo\ncrt 10\n```\n\n";
        }

        return examples.toString();
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    currentField.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // Field separator
                result.add(currentField.toString().trim());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }

        // Add the last field
        result.add(currentField.toString().trim());

        return result.toArray(new String[0]);
    }

    public ChatController(GUIWorkspace workspace) {
        this.workspace = workspace;
        pane = new ChatPane();
        frame = new JFrame("NetLogo Copilot Chat");
        frame.add(pane);
        frame.pack();
        frame.setSize(400, 500);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Simple greeting without system prompt
        pane.appendAssistant("Hello! Ask me to generate or explain NetLogo code.");

        // Set up event listeners
        pane.getInputField().addActionListener(e -> send(pane.getInputField().getText()));
        pane.getSendButton().addActionListener(e -> send(pane.getInputField().getText()));
        pane.getRunCodeButton().addActionListener(e -> runGeneratedCode());
    }

    private void extractAndStoreNetLogoCode(String response) {
        // Extract NetLogo code from the response
        Pattern codePattern = Pattern.compile("```(?:netlogo|nlogo)\\s*\\n?(.*?)```",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = codePattern.matcher(response);

        if (matcher.find()) {
            String newCode = matcher.group(1).trim();
            System.out.println("DEBUG: Extracted NetLogo code: " + newCode);

            // Add to recent code history
            recentNetLogoCode.add(newCode);

            // Keep only the last MAX_CODE_HISTORY items
            if (recentNetLogoCode.size() > MAX_CODE_HISTORY) {
                recentNetLogoCode.remove(0); // Remove oldest
            }

            System.out.println("DEBUG: Code history size: " + recentNetLogoCode.size());
        }
    }

    public void open() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    private void runGeneratedCode() {
        String code = pane.getLastGeneratedCode();
        System.out.println("DEBUG: Attempting to run code: '" + code + "'");

        if (code.isEmpty()) {
            System.out.println("DEBUG: No code to run");
            JOptionPane.showMessageDialog(frame,
                    "No NetLogo code found to run. Please generate some code first.",
                    "No Code to Run",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Disable the button to prevent multiple clicks
        pane.getRunCodeButton().setEnabled(false);
        pane.getRunCodeButton().setText("Running...");

        // Use SwingWorker to execute NetLogo code on a background thread
        new SwingWorker<Void, Void>() {
            private String errorMessage = null;
            private boolean success = false;

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    System.out.println("DEBUG: Executing NetLogo code in background thread");
                    workspace.command(code);
                    System.out.println("DEBUG: NetLogo execution succeeded");
                    success = true;
                } catch (Exception e) {
                    System.out.println("DEBUG: NetLogo execution failed: " + e.getMessage());
                    e.printStackTrace();
                    errorMessage = e.getMessage();
                    success = false;
                }
                return null;
            }

            @Override
            protected void done() {
                // Re-enable the button
                pane.getRunCodeButton().setEnabled(true);
                pane.getRunCodeButton().setText("Run Code");

                if (success) {
                    String successMsg = "✅ **Code executed successfully!**\n\nExecuted code:\n```netlogo\n" + code
                            + "\n```";
                    pane.appendAssistant(successMsg);
                } else {
                    String errorMsg = "❌ **Error executing code:** " + errorMessage +
                            "\n\nAttempted to run:\n```netlogo\n" + code + "\n```";
                    pane.appendAssistant(errorMsg);

                    JOptionPane.showMessageDialog(frame,
                            "Error executing NetLogo code:\n" + errorMessage,
                            "Execution Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void send(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty())
            return;

        pane.clearInput();
        pane.appendUser(trimmed);

        // Disable UI during generation
        pane.setUIEnabled(false);

        // Start streaming message display
        pane.startStreamingMessage();

        new SwingWorker<String, String>() {
            @Override
            protected String doInBackground() {
                try {
                    // Build simple prompt with NetLogo assistant instructions
                    String prompt = buildSimplePrompt(trimmed);
                    List<String> emptyHistory = new ArrayList<>();

                    System.out.println("DEBUG: Sending prompt: " + prompt);

                    Stream<String> tokens = OllamaClient.getInstance()
                            .ask(prompt, emptyHistory) // Send the formatted prompt
                            .map(tokenData -> tokenData.getToken());

                    StringBuilder reply = new StringBuilder();
                    tokens.forEach(token -> {
                        reply.append(token);
                        publish(token);

                        // Add small delay for smoother character-by-character display
                        try {
                            Thread.sleep(10); // 10ms delay between characters
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });

                    return reply.toString();
                } catch (Exception ex) {
                    publish("\n[Error] " + ex.getMessage());
                    return null;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                // Process tokens on EDT for smooth display
                chunks.forEach(pane::appendStreamingToken);
            }

            @Override
            protected void done() {
                try {
                    String fullReply = get();
                    if (fullReply != null) {
                        // Store the raw response for code extraction
                        extractAndStoreNetLogoCode(fullReply);

                        // Add user and assistant messages to conversation history
                        JsonObject userMsg = new JsonObject();
                        userMsg.addProperty("role", "user");
                        userMsg.addProperty("content", trimmed);
                        messages.add(userMsg);

                        JsonObject assistantMsg = new JsonObject();
                        assistantMsg.addProperty("role", "assistant");
                        assistantMsg.addProperty("content", fullReply);
                        messages.add(assistantMsg);
                    }
                } catch (Exception e) {
                    System.err.println("Error getting response: " + e.getMessage());
                } finally {
                    // Finish streaming and re-enable UI
                    pane.finishStreamingMessage();
                    pane.setUIEnabled(true);
                }
            }
        }.execute();
    }
}