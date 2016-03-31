/**
 *
 */
package org.commcare.xml;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.suite.model.Profile;
import org.commcare.util.CommCareInstance;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author ctsims
 */
public class ProfileParser extends ElementParser<Profile> {

    ResourceTable table;
    String resourceId;
    int initialResourceStatus;
    CommCareInstance instance;
    boolean forceVersion = false;

    // Is installation of a new app (that is NOT marked as "ignore", since these can always be
    // installed no matter what) allowed?
    boolean newInstallPermitted;

    // If install is allowed, does this new app itself need to be multiple-apps compatible in order
    // to be installed
    boolean multipleAppCompatibilityRequired;

    public static ProfileParser getMultipleAppsBlindProfileParser(
            InputStream suiteStream, CommCareInstance instance, ResourceTable table,
            String resourceId, int initialResourceStatus, boolean forceVersion)
            throws IOException {

        return new ProfileParser(suiteStream, instance, table, resourceId, initialResourceStatus,
                forceVersion, true, false);
    }

    public ProfileParser(InputStream suiteStream, CommCareInstance instance, ResourceTable table,
            String resourceId, int initialResourceStatus, boolean forceVersion,
                         boolean newInstallPermitted, boolean multipleAppCompatibilityRequired)
            throws IOException {

        super(ElementParser.instantiateParser(suiteStream));
        this.table = table;
        this.resourceId = resourceId;
        this.initialResourceStatus = initialResourceStatus;
        this.instance = instance;
        this.forceVersion = forceVersion;
        this.newInstallPermitted = newInstallPermitted;
        this.multipleAppCompatibilityRequired = multipleAppCompatibilityRequired;
    }

    public Profile parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        checkNode("profile");

        String sVersion = parser.getAttributeValue(null, "version");
        int version = parseInt(sVersion);

        String authRef = parser.getAttributeValue(null, "update");

        String sMajor = parser.getAttributeValue(null, "requiredMajor");
        String sMinor = parser.getAttributeValue(null, "requiredMinor");

        String uniqueId = parser.getAttributeValue(null, "uniqueid");
        String displayName = parser.getAttributeValue(null, "name");

        int major = -1;
        int minor = -1;
        
        if (sMajor != null) {
            major = parseInt(sMajor);
        }


        if (sMinor != null) {
            minor = parseInt(sMinor);
        }

        //If version information is available, check valid versions
        if ((!forceVersion && this.instance != null) && (major != -1) && (minor != -1)) {

            //For the major version, only a matching number is valid, 2.0 cannot be run on either 1.0 or 3.0
            if (this.instance.getMajorVersion() != -1
                    && this.instance.getMajorVersion() != major) {    //changed < to !=

                throw new UnfullfilledRequirementsException(
                        "Major Version Mismatch (Required: " + major + " | Available: " +
                                this.instance.getMajorVersion() + ")",
                        UnfullfilledRequirementsException.SEVERITY_PROMPT,
                        UnfullfilledRequirementsException.REQUIREMENT_MAJOR_APP_VERSION, major,
                        minor, this.instance.getMajorVersion(), this.instance.getMinorVersion());
            }

            //For the minor version, anything greater than the profile's version is valid
            if (this.instance.getMinorVersion() != -1
                    && this.instance.getMinorVersion() < minor) {
                throw new UnfullfilledRequirementsException(
                        "Minor Version Mismatch (Required: " + minor + " | Available: " +
                                this.instance.getMinorVersion() + ")",
                        UnfullfilledRequirementsException.SEVERITY_PROMPT,
                        UnfullfilledRequirementsException.REQUIREMENT_MINOR_APP_VERSION, major,
                        minor, this.instance.getMajorVersion(), this.instance.getMinorVersion());
            }
        }

        String registrationNamespace = null;

        // Handle possibility of the profile file missing fields that are needed for multiple apps support
        // Mark as an old version of the profile if missing either necessary field
        boolean fromOld = (uniqueId == null) || (displayName == null);
        if (uniqueId == null) {
            // Note that this version of unique ID generation will not protect against a user
            // installing the same app multiple times, but this case is sufficiently rare that we are
            // calling that fine. All HQ-generated profile.ccpr files now have the correct fields,
            // so we are only talking about cases where either an app hasn't been updated in a very
            // long time, or someone is doing their own weird thing outside of HQ
            uniqueId = PropertyUtils.genUUID();
        }
        if (displayName == null) {
            // Make the displayName an empty string instead of just null, which will signal the app
            // to use the display name from localizations instead later on
            displayName = "";
        }

        Profile profile = new Profile(version, authRef, uniqueId, displayName, fromOld);
        try {

            // Now that we've covered being inside of the profile,
            // start traversing.
            parser.next();

            int eventType;
            eventType = parser.getEventType();

            do {
                if (eventType == KXmlParser.END_DOCUMENT) {

                } else if (eventType == KXmlParser.START_TAG) {
                    if (parser.getName().toLowerCase().equals("property")) {
                        String key = parser.getAttributeValue(null, "key");
                        String value = parser.getAttributeValue(null, "value");
                        String force = parser.getAttributeValue(null, "force");

                        if (force != null) {
                            if ("true".equals(force.toLowerCase())) {
                                profile.addPropertySetter(key, value, true);
                            } else {
                                profile.addPropertySetter(key, value, false);
                            }
                        } else {
                            profile.addPropertySetter(key, value);
                        }
                    } else if (parser.getName().toLowerCase().equals("root")) {
                        RootTranslator root = new RootParser(this.parser).parse();
                        profile.addRoot(root);
                    } else if (parser.getName().toLowerCase().equals("login")) {
                        // Get the resource block or fail out
                        getNextTagInBlock("login");
                        Resource resource = new ResourceParser(parser, maximumResourceAuthority).parse();
                        table.addResource(resource, table.getInstallers().getLoginImageInstaller(), resourceId, initialResourceStatus);
                    } else if (parser.getName().toLowerCase().equals("features")) {
                        while (nextTagInBlock("features")) {
                            String tag = parser.getName().toLowerCase();
                            String active = parser.getAttributeValue(null, "active");
                            boolean isActive = false;
                            if (active != null && active.toLowerCase().equals("true")) {
                                isActive = true;
                            }
                            if (tag.equals("checkoff")) {

                            } else if (tag.equals("reminders")) {
                                if (nextTagInBlock("reminders")) {
                                    checkNode("time");
                                    String reminderTime = parser.nextText();
                                    int days = this.parseInt(reminderTime);
                                }
                            } else if (tag.equals("package")) {
                                //nothing (yet)
                            } else if (tag.equals("users")) {
                                while (nextTagInBlock("users")) {
                                    if (parser.getName().toLowerCase().equals("registration")) {
                                        registrationNamespace = parser.nextText();
                                        profile.addPropertySetter("user_reg_namespace", registrationNamespace, true);
                                    } else if (parser.getName().toLowerCase().equals("logo")) {
                                        String logo = parser.nextText();
                                        profile.addPropertySetter("cc_login_image", logo, true);
                                    } else {
                                        throw new InvalidStructureException("Unrecognized tag " + parser.getName() + " inside of users feature block", parser);
                                    }
                                }
                            } else if (tag.equals("sense")) {

                            }

                            profile.setFeatureActive(tag, isActive);

                            //TODO: set feature activation in profile
                        }
                    } else if (parser.getName().toLowerCase().equals("suite")) {
                        // Get the resource block or fail out
                        getNextTagInBlock("suite");
                        Resource resource = new ResourceParser(parser, maximumResourceAuthority).parse();

                        //TODO: Possibly add a real parent reference if we decide these go in the table
                        table.addResource(resource, table.getInstallers().getSuiteInstaller(), resourceId, initialResourceStatus);
                    } else {
                        System.out.println("Unrecognized Tag: "
                                + parser.getName());
                    }
                } else if (eventType == KXmlParser.END_TAG) {
                    // we shouldn't ever get this I don't believe, maybe on the
                    // last node?
                } else if (eventType == KXmlParser.TEXT) {
                    // Shouldn't ever get this (Delete the if, if so).
                }
                eventType = parser.next();
            } while (eventType != KXmlParser.END_DOCUMENT);

            verifyMultipleAppsCompliance(profile);

            return profile;

        } catch (XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new InvalidStructureException("Pull Parse Exception, malformed XML.", parser);
        } catch (StorageFullException e) {
            e.printStackTrace();
            //BUT not really! This should maybe be added to the high level declaration
            //instead? Or maybe there should be a more general Resource Management Exception?
            throw new InvalidStructureException("Problem storing parser suite XML", parser);
        }
    }

    int maximumResourceAuthority = -1;

    public void setMaximumAuthority(int authority) {
        maximumResourceAuthority = authority;
    }

    private void verifyMultipleAppsCompliance(Profile profile)
            throws UnfullfilledRequirementsException {

        String compatibilityValue = profile.getMultipleAppsCompatibility();
        if (Profile.MULT_APPS_IGNORE_VALUE.equals(compatibilityValue)) {
            // If the new app is set to "ignore", we can install no matter what
            return;
        } else if (!newInstallPermitted) {
            throw new UnfullfilledRequirementsException("One or more of your currently installed" +
                    "apps are not compatible with multiple apps. In order to install additional" +
                    "apps, you must uninstall or upgrade that app first",
                    UnfullfilledRequirementsException.SEVERITY_PROMPT,
                    UnfullfilledRequirementsException.REQUIREMENT_MULTIPLE_APPS_COMPAT_EXISTING);
        } else if (multipleAppCompatibilityRequired &&
                !compatibilityValue.equals(Profile.MULT_APPS_ENABLED_VALUE)) {
            throw new UnfullfilledRequirementsException("The app you are trying to install is not" +
                    "compatible with multiple apps, and you already have 1 or more apps installed " +
                    "on your device. In order to install this app, you must uninstall all apps " +
                    "currently on your device, or upgrade the project space for this app.",
                    UnfullfilledRequirementsException.SEVERITY_PROMPT,
                    UnfullfilledRequirementsException.REQUIREMENT_MULTIPLE_APPS_COMPAT_NEW);
        } else {
            return;
        }
    }

}