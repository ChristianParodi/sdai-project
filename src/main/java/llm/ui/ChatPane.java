package llm.ui;


import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.JsonObject;
import ollama.OllamaClient;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.nlogo.window.GUIWorkspace;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Swing panel displaying chat history and accepting user input.
 */
class ChatPane extends JPanel {
    private final JEditorPane historyPane = new JEditorPane();
    private final JScrollPane scroll;
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final Parser mdParser = Parser.builder().build();
    private final HtmlRenderer mdRenderer = HtmlRenderer.builder().build();
    private final StringBuilder html = new StringBuilder();

    ChatPane() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        historyPane.setContentType("text/html");
        historyPane.setEditable(false);
        html.append("<html><body style='font-family:JetBrains Mono,monospace;'>");

        scroll = new JScrollPane(historyPane);
        add(scroll, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(4, 0));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
    }

    JTextField getInputField() { return inputField; }
    JButton getSendButton() { return sendButton; }
    void clearInput() { inputField.setText(""); }

    void appendUser(String md) {
        appendMessage("User", md, "transparent", "right");
    }

    void appendAssistant(String md) {
        appendMessage("Assistant", md, "transparent", "left");
    }

    private void appendMessage(String sender, String md, String bgColor, String align) {
        Node doc = mdParser.parse(md);
        String htmlFragment = mdRenderer.render(doc);
        // Append sender label
        html.append(String.format(
                "<div style='font-weight:bold;color:#333;padding:4px 0;text-align:%s;'>%s</div>",
                align, sender
        ));
        // Append message bubble
        html.append(String.format(
                "<div style='background:%s;padding:8px;margin:4px 0;border-radius:4px;text-align:%s;'>%s</div>",
                bgColor, align, htmlFragment
        ));
        historyPane.setText(html.append("</body></html>").toString());
        html.setLength(html.length() - "</body></html>".length());
        SwingUtilities.invokeLater(() -> historyPane.setCaretPosition(historyPane.getDocument().getLength()));
    }
}
