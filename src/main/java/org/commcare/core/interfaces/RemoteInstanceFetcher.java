package org.commcare.core.interfaces;

import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Fetches remote instance definitions from cache or by making a remote web call
 */
public interface RemoteInstanceFetcher {

    TreeElement getExternalRoot(String instanceId, ExternalDataInstanceSource source)
            throws UnfullfilledRequirementsException, XmlPullParserException, InvalidStructureException, IOException;

    class RemoteInstanceException extends Exception {
        public RemoteInstanceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
