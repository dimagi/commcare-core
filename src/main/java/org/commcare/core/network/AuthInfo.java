package org.commcare.core.network;

/**
 * Created by amstone326 on 5/8/18.
 */

public abstract class AuthInfo {

    public String username;
    public String password;

    public static class NoAuth extends AuthInfo {

    }

    public static class ProvidedAuth extends AuthInfo {

        public ProvidedAuth(String username, String password) {
            this.username = username;
            this.password = password;
        }

    }

    // Auth with the currently-logged in user
    public static class CurrentAuth extends AuthInfo {

    }

}
