package org.commcare.suite.model;

import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.ElementParser;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * User restore xml file sometimes present in apps.
 * Used for offline (demo user) logins.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class OfflineUserRestore implements Persistable {
    public static final String STORAGE_KEY = "OfflineUserRestore";
    private int recordId = -1;
    private String restore;
    private String reference;
    private String username;
    private String password;

    public OfflineUserRestore() {
    }

    public OfflineUserRestore(String reference) {
        this.reference = reference;
        this.username = parseUsernameFromRestoreFile(reference);
        this.password = PropertyUtils.genUUID();
    }

    public static OfflineUserRestore buildInMemoryUserRestore(InputStream restoreStream) throws IOException {
        OfflineUserRestore offlineUserRestore = new OfflineUserRestore();
        offlineUserRestore.restore = new String(StreamsUtil.inputStreamToByteArray(restoreStream));

        return offlineUserRestore;
    }

    public InputStream getRestoreStream() {
        if (reference == null) {
            return getInMemoryStream();
        } else {
            return getStreamFromReference();
        }
    }

    private InputStream getStreamFromReference() {
        try {
            Reference local = ReferenceManager._().DeriveReference(reference);
            return local.getStream();
        } catch (IOException | InvalidReferenceException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream getInMemoryStream() {
        try {
            return new ByteArrayInputStream(restore.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getReference() {
        return reference;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        this.recordId = ExtUtil.readInt(in);
        this.reference = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        this.restore = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(reference));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(restore));
    }

    @Override
    public void setID(int ID) {
        recordId = ID;
    }

    @Override
    public int getID() {
        return recordId;
    }

    private static String parseUsernameFromRestoreFile(String localRestoreReference) {
        try {
            InputStream is = ReferenceManager._().DeriveReference(localRestoreReference).getStream();
            KXmlParser parser = ElementParser.instantiateParser(is);
            parser.next();
            int eventType = parser.getEventType();
            do {
                if (eventType == KXmlParser.START_TAG) {
                    if (parser.getName().toLowerCase().equals("username")) {
                        parser.next();
                        return parser.getText();
                    }
                }
                eventType = parser.next();
            } while (eventType != KXmlParser.END_DOCUMENT);
        } catch (IOException | XmlPullParserException | InvalidReferenceException e) {
            return null;
        }
        return null;
    }
}
