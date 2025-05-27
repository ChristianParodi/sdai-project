// NetLogo Class Manager
package myextension;

import org.nlogo.api.*;

public class MyExtension extends DefaultClassManager {
    @Override
    public void load(PrimitiveManager primitiveManager) {
        primitiveManager.addPrimitive("say-hi", new IntegerList());
    }
}
