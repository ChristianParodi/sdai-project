// NetLogo Reporter
package myextension;

import org.nlogo.api.*;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;

public class IntegerList implements Reporter {
    // take a string as input, report a string
    public Syntax getSyntax() {
        var input = new int[] {Syntax.StringType()};
        var output = Syntax.StringType();
        return SyntaxJ.reporterSyntax(input, output);
    }

    public Object report(Argument args[], Context context)
            throws ExtensionException {
        String str = "Ciao";
        return str;
    }
}