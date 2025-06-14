package llm;

import ollama.*;
import java.util.*;
import java.util.stream.*;
import org.nlogo.core.*;

public class ChatSession implements ExtensionObject {
    private final List<Map<String, String>> chatHistory = new ArrayList<>(); // Role-tagged history
    private final List<String> plainHistory = new ArrayList<>(); // raw history to pass to Ollama
    private final String role;
    private final String systemPrompt;

    public ChatSession(String role, String systemPrompt) {
        this.role = role;
        this.systemPrompt = systemPrompt;
        chatHistory.add(Map.of("role", "system", "content", this.systemPrompt));
        plainHistory.add(this.systemPrompt);
    }

    public ChatSession(String role) {
        this(role, defaultSystemPrompt(role));
    }

    public ChatSession() {
        this("turtle");
    }

    private static String defaultSystemPrompt(String role) {
        if ("assistant".equals(role)) {
            return "You are a NetLogo coding assistant. Translate user requests into valid NetLogo code and explanations.";
        } else {
            return "You are a turtle in a 2D world. Always role-play as a turtle. Never break character. Only output what you would say to another turtle. Never mention NetLogo, code, simulation, or AI. Never explain your reasoning. Never use quotation marks. Keep replies to 1-2 friendly sentences.";
        }
    }

    public String getRole() {
        return role;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public Stream<TokenData> ask(Map<String, String> message) {
        chatHistory.add(message);
        plainHistory.add(message.get("content"));

        try {
            // Only send the latest message; let the backend handle the history and system
            // prompt
            Stream<TokenData> responseTokens = OllamaClient.getInstance()
                    .ask(message.get("content"), plainHistory);

            StringBuilder assistantBuilder = new StringBuilder();
            responseTokens.forEach(token -> assistantBuilder.append(token.getToken()));
            String assistantReply = assistantBuilder.toString();

            chatHistory.add(Map.of("role", "assistant", "content", assistantReply));
            plainHistory.add(assistantReply);

            return Stream.of(new TokenData(assistantReply));
        } catch (Exception e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    @Override
    public String getExtensionName() {
        return "llm";
    }

    @Override
    public String getNLTypeName() {
        return "chat";
    }

    @Override
    public boolean recursivelyEqual(Object obj) {
        return false;
    }

    @Override
    public String dump(boolean readable, boolean exporting, boolean references) {
        // return the entire history
        StringBuilder dump = new StringBuilder();
        for (Map<String, String> msg : chatHistory) {
            dump.append(msg.get("role"))
                    .append(": ")
                    .append(msg.get("content"))
                    .append("\n");
        }
        return dump.toString();
    }
}
