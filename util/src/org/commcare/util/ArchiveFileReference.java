/**
 * 
 */
package org.commcare.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.javarosa.core.reference.Reference;

/**
 * @author wspride
 * this class associates a GUID and relative path with a corresponding 
 * real directory in the filesystem
 *
 */
public class ArchiveFileReference implements Reference {

    String GUID;
    String archiveURI;
    ZipFile mZipFile;

    public ArchiveFileReference(ZipFile zipFile, String GUID, String archiveURI) {
        this.archiveURI = archiveURI;
        this.mZipFile = zipFile;
        this.GUID = GUID;
    }

    public boolean doesBinaryExist() throws IOException {
        return true;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Archive references are read only!");
    }

    public InputStream getStream() throws IOException {
        return mZipFile.getInputStream(mZipFile.getEntry(archiveURI));
    }

    public String getURI() {
        return "jr://archive/" + GUID + "/" + archiveURI;
    }

    public boolean isReadOnly() {
        return true;
    }

    public void remove() throws IOException {
        throw new IOException("Cannot remove files from the archive");
    }

    public String getLocalURI() {
        return null;
    }

    public Reference[] probeAlternativeReferences() {
        return new Reference [0];
    }
}
