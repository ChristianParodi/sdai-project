package llm.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.JsonObject;
import ollama.OllamaClient;
import org.nlogo.window.GUIWorkspace;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the chat pane lifecycle and orchestrates LLM calls via OllamaClient.
 */
public class ChatController {
    private final ChatPane pane;
    private final JFrame frame;
    private final List<JsonObject> messages = new ArrayList<>();

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
                "Translate natural-language requests into valid NetLogo 6.4 code." +
                "The NetLogo version may differ if the user tells you to use another one." +
                "Always return two sections: CODE and EXPLANATION in Markdown format. " +
                "Use only built-in NetLogo primitives. \n\n" +
                "Example: User: I want 10 turtles randomly placed. \n" +
                "Assistant: CODE:\n```netlogo\ncrt 10 [ setxy random-xcor random-ycor ]\n```\n" +
                "EXPLANATION:\n- creates ten turtles\n- positions each randomly\n" +
                "Never use bullet lists.";
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", prompt);
        return sys;
    }

    public ChatController(GUIWorkspace workspace) {
        pane = new ChatPane();
        frame = new JFrame("NetLogo Copilot Chat");
        frame.add(pane);
        frame.pack();
        frame.setSize(300, 200);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // initialize conversation
        messages.add(buildSystemPrompt());
        JsonObject greet = new JsonObject();
        greet.addProperty("role", "assistant");
        greet.addProperty("content", "Hello! Ask me to generate or explain NetLogo code.");
        messages.add(greet);
        pane.appendAssistant(greet.get("content").getAsString());

        pane.getInputField().addActionListener(e -> send(pane.getInputField().getText()));
        pane.getSendButton().addActionListener(e -> send(pane.getInputField().getText()));
    }

    public void open() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    private void send(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;
        pane.clearInput();
        pane.appendUser(trimmed);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", trimmed);
        messages.add(userMsg);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                List<String> history = messages.stream()
                        .map(m -> m.get("role").getAsString() + ": " + m.get("content").getAsString())
                        .collect(Collectors.toList());
                try {
                    return OllamaClient.getInstance()
                            .ask(trimmed, history)
                            .map(td -> td.getToken())
                            .collect(Collectors.joining());
                } catch (Exception ex) {
                    return "**Error:** " + ex.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String reply = get();
                    JsonObject assistantMsg = new JsonObject();
                    assistantMsg.addProperty("role", "assistant");
                    assistantMsg.addProperty("content", reply);
                    messages.add(assistantMsg);
                    pane.appendAssistant(reply);
                } catch (Exception ignored) {}
            }
        }.execute();
    }
}