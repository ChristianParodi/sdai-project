package llm;

import org.nlogo.api.*;
import org.nlogo.api.Reporter;
import org.nlogo.core.*;

public class LLMAskSync implements Reporter {
    @Override
    public Object report(Argument[] args, Context context) throws ExtensionException {
        ChatSession session = (ChatSession) args[0].get();
        String prompt = args[1].getString();
        StringBuilder builder = new StringBuilder();
        session.ask(prompt).forEach(token -> builder.append(token.getToken()));
        return builder.toString();
    }

    @Override
    public Syntax getSyntax() {
        int[] input = new int[] {Syntax.AgentType(), Syntax.StringType()};
        int output = Syntax.StringType();
        return SyntaxJ.reporterSyntax(input, output);
    }
}
