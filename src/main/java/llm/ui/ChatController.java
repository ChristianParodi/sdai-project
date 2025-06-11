package llm.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.JsonObject;
import ollama.OllamaClient;
import org.nlogo.api.ExtensionException;
import org.nlogo.window.GUIWorkspace;

import javax.swing.*;

import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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

    private JsonObject buildSystemPrompt() {
        String prompt = "You are a NetLogo coding assistant. " +
                "Translate natural-language requests into valid NetLogo 6.4 code. " +
                "Always return two sections: CODE and EXPLANATION in Markdown format. " +
                "Make sure to surround the NetLogo code with ```netlogo and ```. This is crucial for this to work. " +
                "\n\nCorrect NetLogo syntax examples:" +
                "\n- Create turtles: 'create-turtles 10' or 'crt 10'" +
                "\n- Set turtle color: 'set color red' or 'set color blue'" +
                "\n- Move turtles: 'forward 3' or 'fd 3'" +
                "\n- Ask turtles: 'ask turtles [ commands ]'" +
                "\n- Print values: 'print sum [xcor] of turtles'" +
                "\n- Conditional: 'if condition [ commands ]'" +
                "\n\nExample:\nUser: create 10 blue turtles\n\n" +
                "CODE:\n```netlogo\ncreate-turtles 10 [\n  set color blue\n]\n```\n" +
                "EXPLANATION:\nCreates 10 turtles and sets their color to blue using the create-turtles primitive.\n\n"
                +
                "Use only valid NetLogo primitives. Never use made-up syntax like 'crt circle' or 'find (color red)'.";

        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", prompt);
        return sys;
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

        // initialize conversation with system prompt
        messages.add(buildSystemPrompt());

        JsonObject greet = new JsonObject();
        greet.addProperty("role", "assistant");
        greet.addProperty("content", "Hello! Ask me to generate or explain NetLogo code.");
        messages.add(greet);
        pane.appendAssistant(greet.get("content").getAsString());

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

    private List<String> buildContextMessages(String currentUserInput) {
        List<String> contextMessages = new ArrayList<>();

        // Always include system prompt
        contextMessages.add("system: " + messages.get(0).get("content").getAsString());

        // Include recent NetLogo code history if it exists
        if (!recentNetLogoCode.isEmpty()) {
            StringBuilder codeHistory = new StringBuilder();
            codeHistory.append("assistant: Previous NetLogo code examples:\n");

            for (int i = 0; i < recentNetLogoCode.size(); i++) {
                codeHistory.append("Example ").append(i + 1).append(":\n");
                codeHistory.append("```netlogo\n");
                codeHistory.append(recentNetLogoCode.get(i));
                codeHistory.append("\n```\n");
                if (i < recentNetLogoCode.size() - 1) {
                    codeHistory.append("\n");
                }
            }

            contextMessages.add(codeHistory.toString());
        }

        // Add current user input
        contextMessages.add("user: " + currentUserInput);

        return contextMessages;
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

        // Add user message to messages (for display purposes only)
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", trimmed);
        messages.add(userMsg);

        // Disable UI during generation
        pane.setUIEnabled(false);

        // Start streaming message display
        pane.startStreamingMessage();

        new SwingWorker<String, String>() {
            @Override
            protected String doInBackground() {
                try {
                    // Build simple context: only system prompt + current user request
                    List<String> contextMessages = new ArrayList<>();

                    // Add assistant prompt (always first message in our messages list)
                    contextMessages.add("assistant: " + messages.get(0).get("content").getAsString());

                    // Add current user input
                    contextMessages.add("user: " + trimmed);

                    Stream<String> tokens = OllamaClient.getInstance()
                            .ask(trimmed, contextMessages)
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
                        // Extract and store NetLogo code for next context
                        extractAndStoreNetLogoCode(fullReply);

                        // Add assistant response to messages (for display purposes only)
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