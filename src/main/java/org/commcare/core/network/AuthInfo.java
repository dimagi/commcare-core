package org.commcare.core.network;

import org.commcare.cases.util.StringUtils;

/**
 * Created by amstone326 on 5/8/18.
 */

public abstract class AuthInfo {

    public String username;
    public String password;
    public boolean wrapDomain;

    public String bearerToken;

    public static class NoAuth extends AuthInfo {

    }

    public static class ProvidedAuth extends AuthInfo {
        public ProvidedAuth(String username, String password) {
            this(username, password, true);
        }

        public ProvidedAuth(String username, String password, boolean wrapDomain) {
            if (StringUtils.isEmpty(username)) {
                throw new IllegalArgumentException("ProvidedAuth requires a non-empty username");
            }
            if (StringUtils.isEmpty(password)) {
                throw new IllegalArgumentException("ProvidedAuth requires a non-empty password");
            }
            this.username = username;
            this.password = password;
            this.wrapDomain = wrapDomain;
        }
    }

    // Auth with the currently-logged in user
    public static class CurrentAuth extends AuthInfo {

    }

    public static class TokenAuth extends AuthInfo {
        public TokenAuth(String token) {
            if (StringUtils.isEmpty(token)) {
                throw new IllegalArgumentException("TokenAuth requires a non-empty token");
            }
            bearerToken = token;
        }
    }
}
