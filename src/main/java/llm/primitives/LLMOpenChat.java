package llm.primitives;

import llm.ui.ChatController;
import org.nlogo.api.*;
import org.nlogo.api.Command;
import org.nlogo.core.*;
import org.nlogo.window.GUIWorkspace;

import javax.swing.*;

public class LLMOpenChat implements Command {

    @Override
    public void perform(final Argument[] args, final Context context)
            throws ExtensionException {
        // must run on the AWT thread
        GUIWorkspace gw = (GUIWorkspace) context.workspace();
        SwingUtilities.invokeLater(() -> {
            ChatController controller = new ChatController(gw);
            controller.open();
        });
    }

    @Override
    public Syntax getSyntax() {
        return SyntaxJ.commandSyntax();
    }
}
