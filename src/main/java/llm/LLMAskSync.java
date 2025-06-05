package llm;

import ollama.TokenData;
import org.nlogo.api.*;
import org.nlogo.api.Reporter;
import org.nlogo.core.*;

import java.util.stream.Stream;

public class LLMAskSync implements Reporter {
    @Override
    public Object report(Argument[] args, Context context) throws ExtensionException {
        ChatSession session = (ChatSession) args[0].get();
        String prompt = args[1].getString();
        StringBuilder builder = new StringBuilder();
        Stream<TokenData> tokens = session.ask(prompt);
        tokens.forEach(token -> builder.append(token.getToken()));
        String reply = builder.toString();
        return reply;
    }

    @Override
    public Syntax getSyntax() {
        int[] input = new int[] {Syntax.WildcardType(), Syntax.StringType()};
        int output = Syntax.StringType();
        return SyntaxJ.reporterSyntax(input, output);
    }
}
