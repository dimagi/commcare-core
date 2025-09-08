package org.commcare.suite.model;

import org.commcare.util.CommCarePlatform;
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

    private int recordId = -1;
    private int version;
    private String authRef;
    private Vector<PropertySetter> properties;
    private Vector<RootTranslator> roots;
    private Vector<AndroidPackageDependency> dependencies;
    private Hashtable<String, Boolean> featureStatus;

    private String uniqueId;
    private String displayName;
    private String buildProfileId;

    /**
     * Indicates if this was generated from an old version of the profile file, before fields
     * were added for multiple app seating functionality
     */
    private boolean fromOld;
    private Vector<Credential> credentials;

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
                   boolean fromOld, String buildProfileId) {
        this.version = version;
        this.authRef = authRef;
        this.uniqueId = uniqueId;
        this.displayName = displayName;
        this.buildProfileId = buildProfileId;
        this.fromOld = fromOld;
        properties = new Vector<>();
        roots = new Vector<>();
        dependencies = new Vector<>();
        credentials = new Vector<>();
        featureStatus = new Hashtable<>();
        //turn on default features
        featureStatus.put("users", true);
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
     * @return the buildProfileId for this particular app profile
     */
    public String getBuildProfileId() {
        return buildProfileId;
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
                featureStatus.get(feature);
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
    }

    public PropertySetter[] getPropertySetters() {
        PropertySetter[] setters = new PropertySetter[properties.size()];
        for (int i = 0; i < properties.size(); ++i) {
            setters[i] = properties.elementAt(i);
        }
        return setters;
    }

    public void setFeatureActive(String feature, boolean active) {
        this.featureStatus.put(feature, active);
    }

    public Vector<AndroidPackageDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Vector<AndroidPackageDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public void setCredentials(Vector<Credential> credentials) {
        this.credentials = credentials;
    }

    public Vector<Credential> getCredentials() {
        return credentials;
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
    public void initializeProperties(CommCarePlatform platform, boolean enableForce) {
        PropertyManager propertyManager = platform.getPropertyManager();
        for (PropertySetter setter : properties) {
            String property = propertyManager.getSingularProperty(setter.getKey());
            //We only want to set properties which are undefined or are forced
            if (property == null || (enableForce && setter.force)) {
                propertyManager.setProperty(setter.getKey(), setter.getValue());
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

        properties = (Vector<PropertySetter>)ExtUtil.read(in, new ExtWrapList(PropertySetter.class), pf);
        roots = (Vector<RootTranslator>)ExtUtil.read(in, new ExtWrapList(RootTranslator.class), pf);
        featureStatus = (Hashtable<String, Boolean>)ExtUtil.read(in, new ExtWrapMap(String.class, Boolean.class), pf);
        buildProfileId = ExtUtil.readString(in);
        dependencies = (Vector<AndroidPackageDependency>)ExtUtil.read(in,
                new ExtWrapList(AndroidPackageDependency.class), pf);
        credentials = (Vector<Credential>)ExtUtil.read(in,
                new ExtWrapList(Credential.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeNumeric(out, version);
        ExtUtil.writeString(out, authRef);
        ExtUtil.writeString(out, uniqueId);
        ExtUtil.writeString(out, displayName);
        ExtUtil.writeBool(out, fromOld);

        ExtUtil.write(out, new ExtWrapList(properties));
        ExtUtil.write(out, new ExtWrapList(roots));
        ExtUtil.write(out, new ExtWrapMap(featureStatus));
        ExtUtil.writeString(out, buildProfileId);
        ExtUtil.write(out, new ExtWrapList(dependencies));
        ExtUtil.write(out, new ExtWrapList(credentials));
    }
}
