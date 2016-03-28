package org.javarosa.model.xform;

import org.javarosa.core.api.IModule;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.xpath.XPathParseTool;

public class XFormsModule implements IModule {

    public void registerModule() {
        String[] classes = {
                "org.javarosa.model.xform.XPathReference",
                "org.javarosa.xpath.XPathConditional"
        };

        PrototypeManager.registerPrototypes(classes);
        PrototypeManager.registerPrototypes(XPathParseTool.xpathClasses);
    }

}
