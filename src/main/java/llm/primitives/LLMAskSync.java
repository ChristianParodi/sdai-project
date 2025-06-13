package llm.primitives;

import llm.ChatSession;
import ollama.TokenData;
import org.nlogo.api.*;
import org.nlogo.api.Reporter;
import org.nlogo.core.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import utils.StringUtils;

public class LLMAskSync implements Reporter {
    @Override
    public Object report(Argument[] args, Context context) throws ExtensionException {
        try {
            ChatSession session = (ChatSession) args[0].get();
            String prompt = args[1].getString();

            // Use the correct role for the message
            Map<String, String> message = new HashMap<>();
            message.put("role", session.getRole());
            message.put("content", prompt);

            Stream<TokenData> responseTokens = session.ask(message);

            // collect raw answer
            StringBuilder builder = new StringBuilder();
            responseTokens.forEach(tokenData -> builder.append(tokenData.getToken()));
            String rawReply = builder.toString();

            // unescape and wrap
            String unescaped = StringUtils.unescape(rawReply);
            String wrapped = StringUtils.wrapText(unescaped, 60);

            return wrapped;
        } catch (Exception e) {
            throw new ExtensionException(this.getClass().getSimpleName() + " error: ", e);
        }
    }

    @Override
    public Syntax getSyntax() {
        int[] input = new int[] { Syntax.WildcardType(), Syntax.StringType() };
        int output = Syntax.StringType();
        return SyntaxJ.reporterSyntax(input, output);
    }
}
