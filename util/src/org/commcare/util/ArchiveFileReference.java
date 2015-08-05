package org.commcare.util;

import org.javarosa.core.reference.Reference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipFile;

/**
 * An archive file reference retrieves a binary file from a path within a zip
 * file registerd with the appropriate root.
 *
 * @author ctsims
 */
public class ArchiveFileReference implements Reference {

    private final String GUID;
    private final String archiveURI;
    private final ZipFile mZipFile;

    /**
     * @param zipFile    The host file
     * @param GUID       The guid registered with the existing root
     * @param archiveURI a local path to the file being referenced
     */
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
        return new Reference[0];
    }
}
