package llm.ui;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Swing panel displaying chat history and accepting user input.
 */
class ChatPane extends JPanel {
    private final JEditorPane historyPane = new JEditorPane();
    private final JScrollPane scroll;
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JButton runCodeButton = new JButton("Run Code");
    private final Parser mdParser = Parser.builder().build();
    private final HtmlRenderer mdRenderer = HtmlRenderer.builder().build();
    private final StringBuilder html = new StringBuilder();
    private final StringBuilder currentAssistantMessage = new StringBuilder();
    private String lastGeneratedCode = "";
    private boolean isStreamingMessage = false;
    private boolean isLoading = false;

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

        // Create button panel for Send and Run Code buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.add(runCodeButton);
        buttonPanel.add(sendButton);

        inputPanel.add(buttonPanel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Initially disable the run code button
        runCodeButton.setEnabled(false);
        runCodeButton.setToolTipText("Generate NetLogo code first to enable this button");
    }

    JTextField getInputField() {
        return inputField;
    }

    JButton getSendButton() {
        return sendButton;
    }

    JButton getRunCodeButton() {
        return runCodeButton;
    }

    void clearInput() {
        inputField.setText("");
    }

    String getLastGeneratedCode() {
        return lastGeneratedCode;
    }

    void appendUser(String md) {
        appendMessage("User", md, "transparent", "right");
    }

    void appendAssistant(String md) {
        appendMessage("Assistant", md, "transparent", "left");
        extractAndStoreCode(md);
    }

    void startStreamingMessage() {
        if (!isStreamingMessage) {
            isStreamingMessage = true;
            isLoading = true;
            currentAssistantMessage.setLength(0);

            // Append sender label
            html.append("<div style='font-weight:bold;color:#333;padding:4px 0;text-align:left;'>Assistant</div>");
            // Start message bubble with loading indicator
            html.append(
                    "<div style='background:transparent;padding:8px;margin:4px 0;border-radius:4px;text-align:left;'>");
            html.append("<span style='color:#666;font-style:italic;'>● Generating response...</span>");

            // Update display
            String tempHtml = html.toString() + "</div></body></html>";
            historyPane.setText(tempHtml);
            autoScroll();
        }
    }

    void appendStreamingToken(String token) {
        if (!isStreamingMessage) {
            startStreamingMessage();
        }

        // Remove loading indicator on first token
        if (isLoading) {
            isLoading = false;
            // Remove the loading text by backing up in HTML
            String htmlStr = html.toString();
            int loadingStart = htmlStr
                    .lastIndexOf("<span style='color:#666;font-style:italic;'>● Generating response...</span>");
            if (loadingStart != -1) {
                html.setLength(loadingStart);
            }
        }

        currentAssistantMessage.append(token);

        // Render the current message as markdown
        Node doc = mdParser.parse(currentAssistantMessage.toString());
        String htmlFragment = mdRenderer.render(doc);

        // Update the HTML content with typing cursor
        String typingCursor = "<span style='animation: blink 1s infinite;'>|</span>";
        String tempHtml = html.toString() + htmlFragment + typingCursor + "</div></body></html>";

        // Add CSS for blinking cursor
        tempHtml = tempHtml.replace("<html><body",
                "<html><head><style>@keyframes blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }</style></head><body");

        historyPane.setText(tempHtml);
        autoScroll();
    }

    void finishStreamingMessage() {
        if (isStreamingMessage) {
            isStreamingMessage = false;
            isLoading = false;

            // Render final message without cursor
            Node doc = mdParser.parse(currentAssistantMessage.toString());
            String htmlFragment = mdRenderer.render(doc);

            // Finalize the message in HTML (remove cursor)
            String finalHtml = html.toString() + htmlFragment + "</div>";
            html.setLength(0);
            html.append(finalHtml);

            // Update display
            historyPane.setText(html.toString() + "</body></html>");

            // Extract and store code from the complete message
            extractAndStoreCode(currentAssistantMessage.toString());

            // Clear the current message buffer
            currentAssistantMessage.setLength(0);
            autoScroll();
        }
    }

    // For backward compatibility
    void appendRaw(String token) {
        appendStreamingToken(token);
    }

    void finishAssistantMessage() {
        finishStreamingMessage();
    }

    private void autoScroll() {
        SwingUtilities.invokeLater(() -> {
            historyPane.setCaretPosition(historyPane.getDocument().getLength());
        });
    }

    void setUIEnabled(boolean enabled) {
        sendButton.setEnabled(enabled);
        inputField.setEnabled(enabled);
        if (enabled) {
            sendButton.setText("Send");
        } else {
            sendButton.setText("Generating...");
        }
    }

    private void extractAndStoreCode(String md) {
        // Extract NetLogo code from markdown code blocks
        Pattern codePattern = Pattern.compile("```(?:netlogo|nlogo)\\s*\\n?(.*?)```",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = codePattern.matcher(md);

        if (matcher.find()) {
            String rawCode = matcher.group(1).trim();
            lastGeneratedCode = cleanExtractedCode(rawCode);
            SwingUtilities.invokeLater(() -> {
                runCodeButton.setEnabled(true);
                runCodeButton.setToolTipText("Click to run the generated NetLogo code");
            });
        } else {
            // Also check for CODE: section format
            Pattern codeSectionPattern = Pattern.compile("CODE:\\s*```(?:netlogo|nlogo)?\\s*\\n?(.*?)```",
                    Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher sectionMatcher = codeSectionPattern.matcher(md);

            if (sectionMatcher.find()) {
                String rawCode = sectionMatcher.group(1).trim();
                lastGeneratedCode = cleanExtractedCode(rawCode);
                SwingUtilities.invokeLater(() -> {
                    runCodeButton.setEnabled(true);
                    runCodeButton.setToolTipText("Click to run the generated NetLogo code");
                });
            } else {
                lastGeneratedCode = "";
                SwingUtilities.invokeLater(() -> {
                    runCodeButton.setEnabled(false);
                    runCodeButton.setToolTipText("Generate NetLogo code first to enable this button");
                });
            }
        }
    }

    private String cleanExtractedCode(String rawCode) {
        String cleaned = rawCode.replaceFirst("(?i)^\\s*code:\\s*", "");
        cleaned = cleaned.replaceFirst("(?i)^\\s*netlogo:\\s*", "");
        cleaned = cleaned.replaceFirst("(?i)^\\s*nlogo:\\s*", "");
        cleaned = cleaned.trim();
        cleaned = cleaned.replaceAll("^\\s*\\n+", "");
        return cleaned;
    }

    protected void appendMessage(String sender, String md, String bgColor, String align) {
        Node doc = mdParser.parse(md);
        String htmlFragment = mdRenderer.render(doc);

        html.append(String.format(
                "<div style='font-weight:bold;color:#333;padding:4px 0;text-align:%s;'>%s</div>",
                align, sender));
        html.append(String.format(
                "<div style='background:%s;padding:8px;margin:4px 0;border-radius:4px;text-align:%s;'>%s</div>",
                bgColor, align, htmlFragment));

        historyPane.setText(html.append("</body></html>").toString());
        html.setLength(html.length() - "</body></html>".length());
        autoScroll();
    }
}