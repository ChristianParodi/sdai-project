package llm;

import ollama.*;
import java.util.*;
import java.util.stream.*;
import org.nlogo.core.*;

public class ChatSession implements ExtensionObject {
    private final List<Map<String,String>> chatHistory = new ArrayList<>(); // Role-tagged history
    private final List<String> plainHistory = new ArrayList<>(); // raw history to pass to Ollama

    public Stream<TokenData> ask(Map<String, String> message) {
        chatHistory.add(message);
        plainHistory.add(message.get("content"));

        // combined prompt string from the full history
        StringBuilder combinedPrompt = new StringBuilder();
        for (Map<String, String> msg : chatHistory) {
            if ("turtle".equals(msg.get("role"))) {
                combinedPrompt.append("Turtle: ")
                        .append(msg.get("content"))
                        .append("\n");
            } else {  // assistant (LLM previous reply)
                combinedPrompt.append("Assistant: ")
                        .append(msg.get("content"))
                        .append("\n");
            }
        }

        // ask Ollama with combined prompt
        try {
            Stream<TokenData> responseTokens = OllamaClient.getInstance()
                    .ask(combinedPrompt.toString(), plainHistory);

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

    @Override public String getExtensionName() { return "llm"; }
    @Override public String getNLTypeName()    { return "chat"; }
    @Override public boolean recursivelyEqual(Object obj) { return false; }
    @Override public String dump(boolean readable, boolean exporting, boolean references) {
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
