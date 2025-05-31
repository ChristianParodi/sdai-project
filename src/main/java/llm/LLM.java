// NetLogo Class Manager
package llm;

import org.nlogo.api.*;

public class LLM extends DefaultClassManager {
    @Override
    public void load(PrimitiveManager primitiveManager) {
        primitiveManager.addPrimitive("ask", new LLMAskSync());
        primitiveManager.addPrimitive("create-session", new LLMCreateSession());
    }
}
