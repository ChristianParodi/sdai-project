package llm.primitives;

import org.nlogo.api.*;
import org.nlogo.api.Reporter;
import org.nlogo.core.*;
import java.util.*;
import java.util.concurrent.*;

public class PollConversation implements Reporter {

    @Override
    public Object report(Argument[] args, Context context) throws ExtensionException {
        try {
            String handle = args[0].getString(); // chat ID

            // 2. Look in LLMAskAsync.buffers for this handle
            BlockingQueue<String> queue = LLMAskAsync.buffers.get(handle);
            if (queue == null) {
                // no active conversation: return an empty LogoList
                return LogoList.fromJava(Collections.emptyList());
            }

            // queue to list
            List<String> tokens = new ArrayList<>();
            queue.drainTo(tokens);
            LogoList logoList = LogoList.fromJava(tokens);

            return logoList;
        } catch (Exception e) {
            throw new ExtensionException("PollConversation error: " + e.getMessage());
        }
    }

    @Override
    public Syntax getSyntax() {
        // Input: one string (the handle), Output: a NetLogo list
        return SyntaxJ.reporterSyntax(
                new int[]{Syntax.StringType()},  // single string argument
                Syntax.ListType()                // returns a NetLogo list
        );
    }
}
