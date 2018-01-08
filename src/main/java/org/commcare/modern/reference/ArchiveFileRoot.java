package org.commcare.modern.reference;

import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceFactory;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.util.PropertyUtils;

import java.util.HashMap;
import java.util.zip.ZipFile;

/**
 * @author wspride
 *         This class managers references between GUIDs and the associated path in the file system
 *         To register an archive file with this system call addArchiveFile(filepath) - this will return a GUID
 *         This GUID will allow you to derive files from this location using the ArchiveFileRefernece class
 */
public class ArchiveFileRoot implements ReferenceFactory {

    protected static final HashMap<String, ZipFile> guidToFolderMap = new HashMap<>();

    protected static final int GUID_LENGTH = 10;

    public ArchiveFileRoot() {
    }

    @Override
    public Reference derive(String guidPath) throws InvalidReferenceException {
        return new ArchiveFileReference(guidToFolderMap.get(getGUID(guidPath)), getGUID(guidPath), getPath(guidPath));
    }

    @Override
    public Reference derive(String URI, String context) throws InvalidReferenceException {
        if (context.lastIndexOf('/') != -1) {
            context = context.substring(0, context.lastIndexOf('/') + 1);
        }
        return ReferenceManager.instance().DeriveReference(context + URI);
    }

    @Override
    public boolean derives(String URI) {
        return URI.toLowerCase().startsWith("jr://archive/");
    }

    public String addArchiveFile(ZipFile zipFile) {
        return addArchiveFile(zipFile, null);
    }

    public String addArchiveFile(ZipFile zip, String appId) {
        String mGUID;
        if (appId == null) {
            mGUID = PropertyUtils.genGUID(GUID_LENGTH);
        } else {
            mGUID = appId;
        }
        guidToFolderMap.put(mGUID, zip);
        return mGUID;
    }

    protected String getGUID(String jrpath) {
        String prependRemoved = jrpath.substring("jr://archive/".length());
        int slashindex = prependRemoved.indexOf("/");
        return prependRemoved.substring(0, slashindex);
    }

    protected String getPath(String jrpath) {
        String mGUID = getGUID(jrpath);
        int mIndex = jrpath.indexOf(mGUID);
        return jrpath.substring(mIndex + mGUID.length() + 1);
    }
}
