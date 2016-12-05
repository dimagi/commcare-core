package org.commcare.modern.reference;

import org.javarosa.core.reference.Reference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
        URL url = new URL(uri);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);  //you still need to handle redirect manully.
        HttpURLConnection.setFollowRedirects(true);
        
        return conn.getInputStream();
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
