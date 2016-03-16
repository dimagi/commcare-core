/**
 *
 */
package org.commcare.modern.reference;

import org.javarosa.core.reference.Reference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author ctsims
 *
 */
public class JavaFileReference implements Reference {

    final String localPart;
    final String uri;
    final String authority;

    public JavaFileReference(String localPart, String uri) {
        this(localPart, uri, "file");
    }

    public JavaFileReference(String localPart, String uri, String authority) {
        this.localPart = localPart;
        this.uri = uri;
        this.authority = authority;
    }

    public boolean doesBinaryExist() throws IOException {
        return file().exists();
    }

    public OutputStream getOutputStream() throws IOException {
        return new FileOutputStream(file());
    }

    public InputStream getStream() throws IOException {
        File file = file();
        if(!file.exists()) {
            if(!file.createNewFile()) {
                throw new IOException("Could not create file at URI " + file.getAbsolutePath());
            }
        }
        return new FileInputStream(file);
    }

    public String getURI() {
        return "jr://" + authority + "/" + uri;
    }

    public boolean isReadOnly() {
        return false;
    }

    public void remove() throws IOException {
        File file = file();
        if(!file.delete()) {
            throw new IOException("Could not delete file at URI " + file.getAbsolutePath());
        }
    }

    private File file() {
        return new File(getLocalURI());
    }

    public String getLocalURI() {
        return localPart + File.separator + uri;
    }

    public Reference[] probeAlternativeReferences() {
        return new Reference[0];
    }
}
