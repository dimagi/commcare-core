package org.commcare.resources.model;

import org.commcare.resources.model.installers.LocaleFileInstaller;
import org.commcare.resources.model.installers.LoginImageInstaller;
import org.commcare.resources.model.installers.MediaInstaller;
import org.commcare.resources.model.installers.ProfileInstaller;
import org.commcare.resources.model.installers.SuiteInstaller;
import org.commcare.resources.model.installers.OfflineUserRestoreInstaller;
import org.commcare.resources.model.installers.XFormInstaller;
import org.javarosa.core.services.storage.IStorageIndexedFactory;

/**
 * @author ctsims
 */
public class InstallerFactory {

    private IStorageIndexedFactory storageFactory;

    public InstallerFactory() {}

    public InstallerFactory(IStorageIndexedFactory storageFactory) {
        this.storageFactory = storageFactory;
    }

    public ResourceInstaller getProfileInstaller(boolean forceInstall) {
        return new ProfileInstaller(forceInstall, storageFactory);
    }

    public ResourceInstaller getXFormInstaller() {
        return new XFormInstaller(storageFactory);
    }

    public ResourceInstaller getUserRestoreInstaller() {
        return new OfflineUserRestoreInstaller(this.storageFactory);
    }

    public ResourceInstaller getSuiteInstaller() {
        return new SuiteInstaller(this.storageFactory);
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
