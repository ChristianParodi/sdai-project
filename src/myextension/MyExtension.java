// NetLogo Class Manager
package myextension;

import org.nlogo.api.*;

import java.util.Collections;
import java.util.List;

public class MyExtension extends DefaultClassManager {
    @Override
    public void load(PrimitiveManager primitiveManager) {
        primitiveManager.addPrimitive("sayhi", new IntegerList());
    }
}
