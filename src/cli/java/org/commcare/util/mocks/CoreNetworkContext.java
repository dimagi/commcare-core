package org.commcare.util.mocks;

import org.commcare.util.Base64;
import org.javarosa.core.services.PropertyManager;

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.util.HashMap;

/**
 * Created by ctsims on 8/11/2017.
 */

public class CoreNetworkContext {
    String username;
    String password;

    public CoreNetworkContext(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getFullyQualifiedUsername() {
        String domain = PropertyManager.instance().getSingularProperty("cc_user_domain");

        return username + "@" + domain;

    }

    public void addAuthProperty(HttpURLConnection connection, HashMap<String, String> properties) {
        String encodedCredentials = Base64.encode(String.format("%s:%s", getFullyQualifiedUsername(), password).getBytes());
        String basicAuth = "Basic " + encodedCredentials;
        properties.put("Authorization",basicAuth);
    }

    public void configureProperties(HttpURLConnection connection, HashMap<String, String> properties) {
        for(String key : properties.keySet()) {
            connection.setRequestProperty(key, properties.get(key));
        }
    }

    public String getPlainUsername() {
        return username;
    }

    public String getPassword() {
        return new String(password);
    }
}
