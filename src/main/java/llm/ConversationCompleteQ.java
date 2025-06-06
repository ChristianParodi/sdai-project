package llm;

import org.nlogo.api.*;
import org.nlogo.api.Reporter;
import org.nlogo.core.*;

public class ConversationCompleteQ implements Reporter {

    @Override
    public Object report(Argument[] args, Context context) throws ExtensionException {
        try {
            String handle = args[0].getString(); // chat ID
            Boolean done = LLMAskAsync.completionFlags.get(handle);
            return (done != null && done);
        } catch (Exception e) {
            throw new ExtensionException("ConversationCompleteQ error: " + e.getMessage());
        }
    }

    @Override
    public Syntax getSyntax() {
        return SyntaxJ.reporterSyntax(
                new int[]{Syntax.StringType()},
                Syntax.BooleanType()
        );
    }
}
