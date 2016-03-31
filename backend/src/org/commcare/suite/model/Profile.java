package org.commcare.suite.model;

import org.commcare.xml.ProfileParser;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Profile is a model which defines the operating profile
 * of a CommCare application. An applicaiton's profile
 * defines what CommCare features should be activated,
 * certain properties which should be defined, and
 * any JavaRosa URI reference roots which should be
 * available.
 *
 * @author ctsims
 */
public class Profile implements Persistable {

    public static final String STORAGE_KEY = "PROFILE";
    public static final String FEATURE_REVIEW = "checkoff";
    public static final String FEATURE_USERS = "users";

    public static final String KEY_MULTIPLE_APPS_COMPATIBILITY = "multiple-apps-compatible";
    public static final String MULT_APPS_ENABLED_VALUE = "enabled";
    public static final String MULT_APPS_DISABLED_VALUE = "disabled";
    public static final String MULT_APPS_IGNORE_VALUE = "ignore";

    private int recordId = -1;
    private int version;
    private String authRef;
    private Vector<PropertySetter> properties;
    private Vector<RootTranslator> roots;
    private Hashtable<String, Boolean> featureStatus;

    private String uniqueId;
    private String displayName;
    private String multipleAppsCompatibility;

    /**
     * Indicates if this was generated from an old version of the profile file, before fields
     * were added for multiple app seating functionality
     */
    private boolean fromOld;

    @SuppressWarnings("unused")
    public Profile() {

    }

    /**
     * Creates an application profile with the provided
     * version and authoritative reference URI.
     *
     * @param version The version of this profile which
     *                is represented by this definition.
     * @param authRef A URI which represents the authoritative
     *                source of this profile's master definition. If the
     *                profile definition read at this URI claims a higher
     *                version number than this profile's version, this profile
     *                is obsoleted by it.
     */
    public Profile(int version, String authRef, String uniqueId, String displayName,
                   boolean fromOld) {
        this.version = version;
        this.authRef = authRef;
        this.uniqueId = uniqueId;
        this.displayName = displayName;
        this.fromOld = fromOld;
        properties = new Vector<PropertySetter>();
        roots = new Vector<RootTranslator>();
        featureStatus = new Hashtable<String, Boolean>();

        //turn on default features
        featureStatus.put("users", new Boolean(true));
    }

    @Override
    public int getID() {
        return recordId;
    }

    @Override
    public void setID(int ID) {
        recordId = ID;
    }

    /**
     * @return the uniqueId assigned to this app from HQ
     */
	public String getUniqueId() {
		return this.uniqueId;
	}

    /**
     * @return the displayName assigned to this app from HQ if it was assigned, or an empty string
     * (If this object was generated from an old version of the profile file, there will be no
     * displayName given and this method will return an empty string, signalling CommCareApp to
     * use the app name from Localization strings instead)
     */
	public String getDisplayName() {
	    return this.displayName;
	}

    /**
     * @return if this object was generated from an old version of the profile.ccpr file
     */
	public boolean isOldVersion() {
	    return this.fromOld;
	}

    /**
     * @return The version of this profile which
     * is represented by this definition.
     */
    public int getVersion() {
        return version;
    }

    /**
     * @return A URI which represents the authoritative
     * source of this profile's master definition. If the
     * profile definition read at this URI claims a higher
     * version number than this profile's version, this profile
     * is obsoleted by it.
     */
    public String getAuthReference() {
        return authRef;
    }

    /**
     * Determines whether or not a specific CommCare feature should
     * be active in the current application.
     *
     * @param feature The key of the feature being requested.
     * @return Whether or not in the application being defined
     * by this profile the feature requested should be made available
     * to end users.
     */
    public boolean isFeatureActive(String feature) {
        return featureStatus.containsKey(feature) &&
                featureStatus.get(feature).booleanValue();
    }

    // The below methods should all be replaced by a model builder
    // or a change to how the profile parser works

    public void addRoot(RootTranslator r) {
        this.roots.addElement(r);
    }

    public void addPropertySetter(String key, String value) {
        this.addPropertySetter(key, value, false);
    }

    public void addPropertySetter(String key, String value, boolean force) {
        properties.addElement(new PropertySetter(key, value, force));
        if (KEY_MULTIPLE_APPS_COMPATIBILITY.equals(key)) {
            setMultipleAppsCompatibility(value);
        }
    }

    public PropertySetter[] getPropertySetters() {
        PropertySetter[] setters = new PropertySetter[properties.size()];
        for (int i = 0; i < properties.size(); ++i) {
            setters[i] = properties.elementAt(i);
        }
        return setters;
    }

    public void setFeatureActive(String feature, boolean active) {
        this.featureStatus.put(feature, new Boolean(active));
    }

    private void setMultipleAppsCompatibility(String value) {
        this.multipleAppsCompatibility = value;
    }

    public String getMultipleAppsCompatibility() {
        if (multipleAppsCompatibility == null) {
            return Profile.MULT_APPS_DISABLED_VALUE;
        }
        return multipleAppsCompatibility;
    }

    /**
     * A helper method which initializes the properties specified
     * by this profile definition.
     *
     * Note: This should probably be stored elsewhere, since the operation
     * mutates the model by removing the properties afterwards. Probably
     * in the property installer?
     *
     * NOTE: Moving at earliest opportunity to j2me profile installer
     */
    public void initializeProperties(boolean enableForce) {
        for (PropertySetter setter : properties) {
            String property = PropertyManager._().getSingularProperty(setter.getKey());
            //We only want to set properties which are undefined or are forced
            if (property == null || (enableForce && setter.force)) {
                PropertyManager._().setProperty(setter.getKey(), setter.getValue());
            }
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        recordId = ExtUtil.readInt(in);
        version = ExtUtil.readInt(in);
        authRef = ExtUtil.readString(in);
        uniqueId = ExtUtil.readString(in);
        displayName = ExtUtil.readString(in);
        fromOld = ExtUtil.readBool(in);
        multipleAppsCompatibility = ExtUtil.readString(in);

        properties = (Vector<PropertySetter>)ExtUtil.read(in, new ExtWrapList(PropertySetter.class), pf);
        roots = (Vector<RootTranslator>)ExtUtil.read(in, new ExtWrapList(RootTranslator.class), pf);
        featureStatus = (Hashtable<String, Boolean>)ExtUtil.read(in, new ExtWrapMap(String.class, Boolean.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeNumeric(out, version);
        ExtUtil.writeString(out, authRef);
        ExtUtil.writeString(out, uniqueId);
        ExtUtil.writeString(out, displayName);
        ExtUtil.writeBool(out, fromOld);
        ExtUtil.writeString(out, getMultipleAppsCompatibility());

        ExtUtil.write(out, new ExtWrapList(properties));
        ExtUtil.write(out, new ExtWrapList(roots));
        ExtUtil.write(out, new ExtWrapMap(featureStatus));
    }
}
