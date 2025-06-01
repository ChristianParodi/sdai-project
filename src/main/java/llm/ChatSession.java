package llm;

import ollama.*;
import java.util.*;
import java.util.stream.*;
import org.nlogo.core.*;

public class ChatSession implements ExtensionObject {
    private final List<String> history = new ArrayList<>();

    public Stream<TokenData> ask(String prompt) {
        history.add("You: " + prompt);
        try {
            Stream<TokenData> response = OllamaClient.getInstance().ask(prompt, history);
            return response.peek(token -> history.add("LLM: " + token.getToken()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Stream.empty();
    }

    // Needed for NetLogo to recognize this as a custom object
    @Override public String getExtensionName() { return "src/llm"; }
    @Override public String getNLTypeName() { return "chat"; }
    @Override public boolean recursivelyEqual(Object obj) { return false; }
    @Override public String dump(boolean readable, boolean exporting, boolean reference) { return String.join("\n", history); }
}
