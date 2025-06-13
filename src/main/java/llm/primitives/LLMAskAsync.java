package llm.primitives;

import llm.ChatSession;
import ollama.TokenData;
import org.nlogo.api.*;
import org.nlogo.core.SyntaxJ;
import org.nlogo.core.Syntax;

import java.util.concurrent.*;
import java.util.*;
import java.util.stream.Stream;

import utils.StringUtils;

public class LLMAskAsync implements Reporter {
    private static final ExecutorService executor = Executors.newCachedThreadPool(); // thread pool

    protected static final ConcurrentHashMap<String, BlockingQueue<String>> buffers = new ConcurrentHashMap<>(); // buffered
                                                                                                                 // tokens
    protected static final ConcurrentHashMap<String, Boolean> completionFlags = new ConcurrentHashMap<>(); // chat
                                                                                                           // completion
                                                                                                           // tracker

    @Override
    public Object report(Argument[] args, Context context) throws ExtensionException {
        try {
            ChatSession session = (ChatSession) args[0].get();
            String prompt = args[1].getString();
            String handle = UUID.randomUUID().toString(); // Chat ID

            BlockingQueue<String> queue = new LinkedBlockingQueue<>();
            buffers.put(handle, queue);
            completionFlags.put(handle, false);

            // Use the correct role for the message
            Map<String, String> message = new HashMap<>();
            message.put("role", session.getRole());
            message.put("content", prompt);

            executor.submit(() -> {
                try {
                    // ask to llama
                    Stream<TokenData> responseTokens = session.ask(message);

                    // collect the response
                    StringBuilder builder = new StringBuilder();
                    responseTokens.forEach(tokenData -> builder.append(tokenData.getToken()));
                    String rawReply = builder.toString();

                    // unescape and wrap
                    String unescaped = StringUtils.unescape(rawReply);
                    String wrapped = StringUtils.wrapText(unescaped, 60);

                    // stream char by char
                    for (int i = 0; i < wrapped.length(); i++) {
                        queue.offer(String.valueOf(wrapped.charAt(i)));
                    }
                } catch (Exception e) {
                    // put an error marker if an error occurs
                    queue.offer("[[ERROR: " + e.getMessage() + "]]");
                } finally {
                    // conversation complete
                    completionFlags.put(handle, true);
                }
            });

            return handle;
        } catch (Exception ex) {
            throw new ExtensionException("LLMAskAsync error: " + ex.getMessage());
        }
    }

    @Override
    public Syntax getSyntax() {
        // Inputs: (ChatSession, String prompt) -> Output: String handle
        return SyntaxJ.reporterSyntax(
                new int[] { Syntax.WildcardType(), Syntax.StringType() },
                Syntax.StringType());
    }
}
