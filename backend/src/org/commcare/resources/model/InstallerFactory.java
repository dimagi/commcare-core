package org.commcare.resources.model;

import org.commcare.resources.model.installers.LocaleFileInstaller;
import org.commcare.resources.model.installers.LoginImageInstaller;
import org.commcare.resources.model.installers.MediaInstaller;
import org.commcare.resources.model.installers.ProfileInstaller;
import org.commcare.resources.model.installers.SuiteInstaller;
import org.commcare.resources.model.installers.UserRestoreInstaller;
import org.commcare.resources.model.installers.XFormInstaller;

/**
 * @author ctsims
 */
public class InstallerFactory {

    public ResourceInstaller getProfileInstaller(boolean forceInstall) {
        return new ProfileInstaller(forceInstall);
    }

    public ResourceInstaller getXFormInstaller() {
        return new XFormInstaller();
    }

    public ResourceInstaller getUserRestoreInstaller() {
        return new UserRestoreInstaller();
    }

    public ResourceInstaller getSuiteInstaller() {
        return new SuiteInstaller();
    }

    public ResourceInstaller getLocaleFileInstaller(String locale) {
        return new LocaleFileInstaller(locale);
    }

    public ResourceInstaller getLoginImageInstaller() {
        return new LoginImageInstaller();
    }

    public ResourceInstaller getMediaInstaller(String path) {
        return new MediaInstaller();
    }
}
