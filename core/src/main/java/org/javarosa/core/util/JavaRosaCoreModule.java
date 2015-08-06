package org.javarosa.core.util;

import org.javarosa.core.api.IModule;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.ResourceReferenceFactory;
import org.javarosa.core.services.PrototypeManager;

/**
 * @author Clayton Sims
 * @date Jun 1, 2009
 */
public class JavaRosaCoreModule implements IModule {

    /* (non-Javadoc)
     * @see org.javarosa.core.api.IModule#registerModule(org.javarosa.core.Context)
     */
    public void registerModule() {
        String[] classes = {
                "org.javarosa.core.services.locale.ResourceFileDataSource",
                "org.javarosa.core.services.locale.TableLocaleSource"
        };
        PrototypeManager.registerPrototypes(classes);
        ReferenceManager._().addReferenceFactory(new ResourceReferenceFactory());
    }
}
