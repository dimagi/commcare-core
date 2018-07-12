package org.commcare.resources.model.installers;

import org.commcare.util.CommCarePlatform;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * TODO: This should possibly just be replaced by a basic file installer along
 * with a reference for the login screen. We'll see.
 *
 * @author ctsims
 */
public class LoginImageInstaller extends BasicInstaller {

    @Override
    public boolean initialize(CommCarePlatform platform, boolean isUpgrade) throws
            IOException, InvalidReferenceException, InvalidStructureException,
            XmlPullParserException, UnfullfilledRequirementsException {
        //Tell the login screen where to get this?
        return true;
    }

    @Override
    public boolean requiresRuntimeInitialization() {
        return true;
    }
}
