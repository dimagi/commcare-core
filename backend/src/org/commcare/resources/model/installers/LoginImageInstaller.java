/**
 *
 */
package org.commcare.resources.model.installers;

import org.commcare.resources.model.ResourceInitializationException;

/**
 * TODO: This should possibly just be replaced by a basic file installer along
 * with a reference for the login screen. We'll see.
 *
 * @author ctsims
 */
public class LoginImageInstaller extends BasicInstaller {

    /* (non-Javadoc)
     * @see org.commcare.resources.model.ResourceInstaller#initialize()
     */
    public boolean initialize() throws ResourceInitializationException {
        //Tell the login screen where to get this?
        return true;
    }

    /* (non-Javadoc)
     * @see org.commcare.resources.model.ResourceInstaller#requiresRuntimeInitialization()
     */
    public boolean requiresRuntimeInitialization() {
        return true;
    }
}
