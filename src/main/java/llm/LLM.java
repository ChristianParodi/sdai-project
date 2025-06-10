// NetLogo Class Manager
package llm;

import llm.primitives.*;
import org.nlogo.api.*;

public class LLM extends DefaultClassManager {
    @Override
    public void load(PrimitiveManager primitiveManager) {
        primitiveManager.addPrimitive("ask", new LLMAskSync());
        primitiveManager.addPrimitive("create-session", new LLMCreateSession());
        primitiveManager.addPrimitive("ask-async", new LLMAskAsync());
        primitiveManager.addPrimitive("poll-conversation", new PollConversation());
        primitiveManager.addPrimitive("conversation-complete?", new ConversationCompleteQ());
        // chat
        primitiveManager.addPrimitive("open-chat", new LLMOpenChat());
    }
}
