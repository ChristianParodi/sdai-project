package llm.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.JsonObject;
import ollama.OllamaClient;
import org.nlogo.window.GUIWorkspace;

import javax.swing.*;
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
    private String systemPrompt = ChatControllerConfig.DEFAULT_SYSTEM_PROMPT;

    static {
        // Apply FlatLaf theme
        System.setProperty("flatlaf.uiScale", "1.0");
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("FlatLaf init failed: " + e.getMessage());
        }
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
                    // Place system prompt and examples BEFORE the user query
                    String prompt = systemPrompt + "\nUser request:\n" + trimmed;
                    List<String> emptyHistory = new ArrayList<>();

                    System.out.println("DEBUG: Sending prompt: " + prompt);

                    Stream<String> tokens = OllamaClient.getInstance()
                            .ask(prompt, emptyHistory) // Send the system prompt and user query
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