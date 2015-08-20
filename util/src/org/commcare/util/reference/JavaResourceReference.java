package org.commcare.util.reference;

import org.javarosa.core.reference.Reference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A resource type for Java classloader resource files
 *
 * Created by ctsims on 8/14/2015.
 */
public class JavaResourceReference implements Reference {

    private final String mLocalPart;
    private final Class mHost;

    public JavaResourceReference (String localPart, Class host) {
        mLocalPart = localPart;
        mHost = host;
    }


    public boolean doesBinaryExist() throws IOException {
        return true;
    }


    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Resource references are read only!");
    }


    public InputStream getStream() throws IOException {
        return mHost.getResourceAsStream(mLocalPart);
    }


    public String getURI() {
        return "jr://resource" + mLocalPart;
    }


    public boolean isReadOnly() {
        return true;
    }


    public void remove() throws IOException {
        throw new IOException("Resource references are read only!");
    }


    public String getLocalURI() {
        return null;
    }


    public Reference[] probeAlternativeReferences() {
        return new Reference[0];
    }
}
