package org.javarosa.core.model;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.util.restorable.Restorable;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Peristable object representing a CommCare mobile user.
 *
 * @author ctsims
 * @author wspride
 */
public class User implements Persistable, Restorable, IMetaData {
    public static final String STORAGE_KEY = "USER";

    public static final String ADMINUSER = "admin";
    public static final String STANDARD = "standard";
    public static final String DEMO_USER = "demo_user";
    public static final String KEY_USER_TYPE = "user_type";
    public static final String TYPE_DEMO = "demo";

    public static final String META_UID = "uid";
    public static final String META_USERNAME = "username";
    public static final String META_ID = "userid";
    public static final String META_WRAPPED_KEY = "wrappedkey";
    public static final String META_SYNC_TOKEN = "synctoken";

    public int recordId = -1; //record id on device
    private String username;
    private String passwordHash;
    private String uniqueId;  //globally-unique id

    static private User demo_user;

    public boolean rememberMe = false;

    public String syncToken;

    public byte[] wrappedKey;

    public Hashtable<String, String> properties = new Hashtable<>();

    public User() {
        setUserType(STANDARD);
    }

    public User(String name, String passw, String uniqueID) {
        this(name, passw, uniqueID, STANDARD);
    }

    public User(String name, String passw, String uniqueID, String userType) {
        username = name;
        passwordHash = passw;
        uniqueId = uniqueID;
        setUserType(userType);
        rememberMe = false;
    }

    // fetch the value for the default user and password from the RMS
    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        this.username = ExtUtil.readString(in);
        this.passwordHash = ExtUtil.readString(in);
        this.recordId = ExtUtil.readInt(in);
        this.uniqueId = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        this.rememberMe = ExtUtil.readBool(in);
        this.syncToken = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        this.properties = (Hashtable)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf);
        this.wrappedKey = ExtUtil.nullIfEmpty(ExtUtil.readBytes(in));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, username);
        ExtUtil.writeString(out, passwordHash);
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(uniqueId));
        ExtUtil.writeBool(out, rememberMe);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(syncToken));
        ExtUtil.write(out, new ExtWrapMap(properties));
        ExtUtil.writeBytes(out, ExtUtil.emptyIfNull(wrappedKey));
    }

    public boolean isAdminUser() {
        return ADMINUSER.equals(getUserType());
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    @Override
    public void setID(int recordId) {
        this.recordId = recordId;
    }

    @Override
    public int getID() {
        return recordId;
    }

    public String getUserType() {
        if (properties.containsKey(KEY_USER_TYPE)) {
            return properties.get(KEY_USER_TYPE);
        } else {
            return null;
        }
    }

    public void setUserType(String userType) {
        properties.put(KEY_USER_TYPE, userType);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public void setUuid(String uuid) {
        this.uniqueId = uuid;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Enumeration listProperties() {
        return this.properties.keys();
    }

    public void setProperty(String key, String val) {
        this.properties.put(key, val);
    }

    public String getProperty(String key) {
        return this.properties.get(key);
    }

    public Hashtable<String, String> getProperties() {
        return this.properties;
    }

    @Override
    public String getRestorableType() {
        return "user";
    }

    @Override
    public void templateData(FormInstance dm, TreeReference parentRef) {
        RestoreUtils.applyDataType(dm, "name", parentRef, String.class);
        RestoreUtils.applyDataType(dm, "pass", parentRef, String.class);
        RestoreUtils.applyDataType(dm, "type", parentRef, String.class);
        RestoreUtils.applyDataType(dm, "user-id", parentRef, Integer.class);
        RestoreUtils.applyDataType(dm, "uuid", parentRef, String.class);
        RestoreUtils.applyDataType(dm, "remember", parentRef, Boolean.class);
    }

    @Override
    public Object getMetaData(String fieldName) {
        if (META_UID.equals(fieldName)) {
            return uniqueId;
        } else if(META_USERNAME.equals(fieldName)) {
            return username;
        } else if(META_ID.equals(fieldName)) {
            return new Integer(recordId);
        } else if (META_WRAPPED_KEY.equals(fieldName)) {
            return wrappedKey;
        } else if (META_SYNC_TOKEN.equals(fieldName)) {
            return ExtUtil.emptyIfNull(syncToken);
        }
        throw new IllegalArgumentException("No metadata field " + fieldName + " for User Models");
    }
    // TODO: Add META_WRAPPED_KEY back in?
    @Override
    public String[] getMetaDataFields() {
        return new String[] {META_UID, META_USERNAME, META_ID, META_SYNC_TOKEN};
    }

    //Don't ever save!
    private String cachedPwd;
    public void setCachedPwd(String password) {
        this.cachedPwd = password;
    }
    public String getCachedPwd() {
        return this.cachedPwd;
    }

    public String getLastSyncToken() {
        return syncToken;
    }

    public void setLastSyncToken(String syncToken) {
        this.syncToken = syncToken;
    }

    public static User FactoryDemoUser() {
        if (demo_user == null) {
            demo_user = new User();
            demo_user.setUsername(User.DEMO_USER); // NOTE: Using a user type as a
            // username also!
            demo_user.setUserType(User.DEMO_USER);
            demo_user.setUuid(User.DEMO_USER);
        }
        return demo_user;
    }

    public void setWrappedKey(byte[] key) {
        this.wrappedKey = key;
    }

    public byte[] getWrappedKey() {
        return wrappedKey;
    }

}
