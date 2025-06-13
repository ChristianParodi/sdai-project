package llm.primitives;

import llm.ChatSession;
import org.nlogo.api.*;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;

public class LLMCreateSession implements Reporter {
    @Override
    public Object report(Argument[] args, Context context) throws ExtensionException {
        // If two string arguments: role and systemPrompt
        if (args.length > 1 && args[0].get() instanceof String && args[1].get() instanceof String) {
            String role = args[0].getString();
            String systemPrompt = args[1].getString();
            return new ChatSession(role, systemPrompt);
        } else if (args.length > 0 && args[0].get() instanceof String) {
            String role = args[0].getString();
            return new ChatSession(role);
        } else {
            return new ChatSession(); // default to 'turtle'
        }
    }

    @Override
    public Syntax getSyntax() {
        // Accepts one or two string arguments (role, systemPrompt)
        return SyntaxJ.reporterSyntax(
                new int[] { Syntax.StringType(), Syntax.StringType() },
                Syntax.WildcardType());
    }
}
