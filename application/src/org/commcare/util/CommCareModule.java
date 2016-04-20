/**
 *
 */
package org.commcare.util;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.installers.BasicInstaller;
import org.commcare.resources.model.installers.LocaleFileInstaller;
import org.commcare.resources.model.installers.MediaInstaller;
import org.commcare.resources.model.installers.ProfileInstaller;
import org.commcare.resources.model.installers.SuiteInstaller;
import org.commcare.resources.model.installers.XFormInstaller;
import org.commcare.suite.model.FormEntry;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.PropertySetter;
import org.commcare.suite.model.Suite;
import org.commcare.suite.model.Text;
import org.commcare.xml.DummyGraphParser.DummyGraphDetailTemplate;
import org.javarosa.core.api.IModule;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.StorageManager;

/**
 * @author ctsims
 *
 */
public class CommCareModule implements IModule {

    /* (non-Javadoc)
     * @see org.javarosa.core.api.IModule#registerModule()
     */
    public void registerModule() {
        String[] prototypes = new String[] {BasicInstaller.class.getName(),
                                            LocaleFileInstaller.class.getName(),
                                            SuiteInstaller.class.getName(),
                                            ProfileInstaller.class.getName(),
                                            MediaInstaller.class.getName(),
                                            XFormInstaller.class.getName(),
                                            Text.class.getName(),
                                            PropertySetter.class.getName(),
                                            FormEntry.class.getName(),
                                            DummyGraphDetailTemplate.class.getName()};
        PrototypeManager.registerPrototypes(prototypes);

        StorageManager.registerStorage(CommCareContext.STORAGE_TABLE_GLOBAL, Resource.class);
        StorageManager.registerStorage(Suite.STORAGE_KEY, Suite.class);
        StorageManager.registerStorage(Profile.STORAGE_KEY, Profile.class);
    }
}
