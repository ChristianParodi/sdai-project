package llm.primitives;

import org.nlogo.api.*;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;

/**
 * LLMRunCode extension primitive: executes a NetLogo code snippet at runtime.
 * Usage in NetLogo interface:
 *   llm:run-code "crt 10 [ fd 1 ]"
 * This will run the generated code directly in the model.
 */
public class LLMRunCode implements Command {

    @Override
    public Syntax getSyntax() {
        // Single string argument containing NetLogo code to run
        return SyntaxJ.commandSyntax(new int[]{ Syntax.StringType() });
    }

    @Override
    public void perform(Argument[] args, Context context) throws ExtensionException {
        try {
            String code = args[0].getString();

            // Validate that code is not empty or null
            if (code == null || code.trim().isEmpty()) {
                throw new ExtensionException("Code string cannot be empty");
            }

            // Execute the code string in the current workspace
            // Use command() method which is simpler and more reliable
            context.workspace().command(code);

        } catch (ExtensionException ex) {
            // Propagate NetLogo extension exceptions directly
            throw ex;
        } catch (LogoException ex) {
            // Handle NetLogo-specific exceptions (runtime errors, compilation errors, etc.)
            throw new ExtensionException("NetLogo error while running code: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            // Wrap any other unexpected exceptions
            throw new ExtensionException("Unexpected error running code: " + ex.getMessage(), ex);
        }
    }
}