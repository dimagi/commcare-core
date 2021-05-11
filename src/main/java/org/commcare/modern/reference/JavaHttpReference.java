package org.commcare.modern.reference;

import org.commcare.core.network.CaptivePortalRedirectException;
import org.commcare.util.NetworkStatus;
import org.javarosa.core.reference.Reference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * @author ctsims
 */
public class JavaHttpReference implements Reference {

    private final String uri;

    public JavaHttpReference(String uri) {
        this.uri = uri;
    }

    @Override
    public boolean doesBinaryExist() throws IOException {
        //For now....
        return true;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Http references are read only!");
    }

    @Override
    public InputStream getStream() throws IOException {
        try {
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);  //you still need to handle redirect manully.
            HttpURLConnection.setFollowRedirects(true);

            return conn.getInputStream();
        } catch (SSLHandshakeException | SSLPeerUnverifiedException e) {
            if(NetworkStatus.isCaptivePortal()) {
                throw new CaptivePortalRedirectException(e);
            }
            throw e;
        }
    }

    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public void remove() throws IOException {
        throw new IOException("Http references are read only!");
    }

    @Override
    public String getLocalURI() {
        return uri;
    }
}
