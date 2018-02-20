package org.commcare.suite.model;

import org.commcare.core.parse.UserXmlParser;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.User;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManagerHandler;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
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
 * @author Aliza Stone (astone@dimagi.com)
 */
public class OfflineUserRestore implements Persistable {
    public static final String STORAGE_KEY = "OfflineUserRestore";
    public static final String DEMO_USER_PASSWORD = "demo-user-password";
    private int recordId = -1;
    private String restore;
    private String reference;
    private String username;

    public OfflineUserRestore() {
    }

    public OfflineUserRestore(String reference)
            throws UnfullfilledRequirementsException, IOException, InvalidStructureException,
            XmlPullParserException, InvalidReferenceException {
        this.reference = reference;
        checkThatRestoreIsValidAndSetUsername();
    }

    public static OfflineUserRestore buildInMemoryUserRestore(InputStream restoreStream)
            throws UnfullfilledRequirementsException, IOException, InvalidStructureException,
            XmlPullParserException {

        OfflineUserRestore offlineUserRestore = new OfflineUserRestore();
        byte[] restoreBytes = StreamsUtil.inputStreamToByteArray(restoreStream);
        offlineUserRestore.restore = new String(restoreBytes);
        offlineUserRestore.checkThatRestoreIsValidAndSetUsername();

        return offlineUserRestore;
    }

    public InputStream getRestoreStream() {
        if (reference != null) {
            // user restore xml was installed to a file
            return getStreamFromReference();
        } else {
            // user restore xml was installed in memory (CLI)
            return getInMemoryStream();
        }
    }

    private InputStream getInMemoryStream() {
        try {
            return new ByteArrayInputStream(restore.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream getStreamFromReference() {
        try {
            Reference local = ReferenceManagerHandler.instance().DeriveReference(reference);
            return local.getStream();
        } catch (IOException | InvalidReferenceException e) {
            throw new RuntimeException(e);
        }
    }

    public String getReference() {
        return reference;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        this.recordId = ExtUtil.readInt(in);
        this.reference = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        this.restore = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        this.username = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(reference));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(restore));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(username));
    }

    @Override
    public void setID(int ID) {
        recordId = ID;
    }

    @Override
    public int getID() {
        return recordId;
    }

    private void checkThatRestoreIsValidAndSetUsername()
            throws UnfullfilledRequirementsException, IOException, InvalidStructureException,
            XmlPullParserException {

        TransactionParserFactory factory = new TransactionParserFactory() {
            @Override
            public TransactionParser getParser(KXmlParser parser) {
                String name = parser.getName();
                if ("registration".equals(name.toLowerCase())) {
                    return buildUserParser(parser);
                }
                return null;
            }
        };

        DataModelPullParser parser = new DataModelPullParser(getRestoreStream(), factory, true, false);
        parser.parse();
    }

    private TransactionParser buildUserParser(KXmlParser parser) {
        return new UserXmlParser(parser) {

            @Override
            protected void commit(User parsed) throws IOException, InvalidStructureException {
                if (!parsed.getUserType().equals(User.TYPE_DEMO)) {
                    throw new InvalidStructureException(
                            "Demo user restore file must be for a user with user_type set to demo");
                }
                if ("".equals(parsed.getUsername()) || parsed.getUsername() == null) {
                    throw new InvalidStructureException(
                            "Demo user restore file must specify a username in the Registration block");
                } else {
                    OfflineUserRestore.this.username = parsed.getUsername();
                }
            }

            @Override
            public User retrieve(String entityId) {
                return null;
            }

        };
    }
}
