package org.commcare.resources.model.installers;

import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.util.CommCareInstance;

/**
 * TODO: This should possibly just be replaced by a basic file installer along
 * with a reference for the login screen. We'll see.
 *
 * @author ctsims
 */
public class LoginImageInstaller extends BasicInstaller {

    @Override
    public boolean initialize(CommCareInstance instance, boolean isUpgrade) throws ResourceInitializationException {
        //Tell the login screen where to get this?
        return true;
    }

    @Override
    public boolean requiresRuntimeInitialization() {
        return true;
    }
}
