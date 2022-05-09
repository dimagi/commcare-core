package org.commcare.core.interfaces;

import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.TreeElement;

/**
 * Fetches remote instance definitions from cache or by making a remote web call
 */
public interface RemoteInstanceFetcher {

    TreeElement getExternalRoot(String instanceId, ExternalDataInstanceSource source) throws RemoteInstanceException;

    class RemoteInstanceException extends Exception {

        public RemoteInstanceException(String message) {
            super(message);
        }

        public RemoteInstanceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
