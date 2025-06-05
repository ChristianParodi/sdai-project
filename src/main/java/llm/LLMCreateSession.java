package llm;

import org.nlogo.api.*;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;

public class LLMCreateSession implements Reporter {
    @Override
    public Object report(Argument[] args, Context context) throws ExtensionException {
        return new ChatSession(); // Assuming ChatSession is your session management class
    }

    @Override
    public Syntax getSyntax() {
        return SyntaxJ.reporterSyntax(new int[] {}, Syntax.WildcardType());
    }
}
