/**
 *
 */
package org.commcare.util;

import org.javarosa.service.transport.securehttp.HttpCredentialProvider;

/**
 * @author ctsims
 *
 */
public class CommCareUserCredentialProvider implements HttpCredentialProvider {

    HttpCredentialProvider derived;
    String domain;

    public CommCareUserCredentialProvider(HttpCredentialProvider derived, String domain) {
        this.derived = derived;
        this.domain = domain;
    }

    /* (non-Javadoc)
     * @see org.javarosa.service.transport.securehttp.HttpCredentialProvider#acquireCredentials()
     */
    public boolean acquireCredentials() {
        return derived.acquireCredentials();
    }

    /* (non-Javadoc)
     * @see org.javarosa.service.transport.securehttp.HttpCredentialProvider#getUsername()
     */
    public String getUsername() {
        if(domain == null) {
            return derived.getUsername();
        }else {
            return derived.getUsername().toLowerCase() + "@" + domain;
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.service.transport.securehttp.HttpCredentialProvider#getPassword()
     */
    public String getPassword() {
        return derived.getPassword();
    }

}
