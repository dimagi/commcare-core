/**
 *
 */
package org.commcare.util;

import org.javarosa.service.transport.securehttp.HttpCredentialProvider;
import org.javarosa.core.model.User;

/**
 * @author ctsims
 *
 */
public class UserCredentialProvider implements HttpCredentialProvider {

    private User user;

    public UserCredentialProvider(User user) {
        this.user = user;
    }

    /* (non-Javadoc)
     * @see org.javarosa.service.transport.securehttp.HttpCredentialProvider#acquireCredentials()
     */
    public boolean acquireCredentials() {
        if(user != null) {
            return true;
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.service.transport.securehttp.HttpCredentialProvider#getPassword()
     */
    public String getPassword() {
        return user.getPassword();
    }

    /* (non-Javadoc)
     * @see org.javarosa.service.transport.securehttp.HttpCredentialProvider#getUsername()
     */
    public String getUsername() {
        return user.getUsername();
    }

}
