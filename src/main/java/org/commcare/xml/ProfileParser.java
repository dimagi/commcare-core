/**
 *
 */
package org.commcare.xml;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.suite.model.AndroidPackageDependency;
import org.commcare.suite.model.Credential;
import org.commcare.suite.model.Profile;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * @author ctsims
 */
public class ProfileParser extends ElementParser<Profile> {

    private static final String NAME_DEPENDENCIES = "dependencies";
    private static final String NAME_ANDROID_PACKAGE = "android_package";
    private static final String NAME_CREDENTIALS = "credentials";
    private static final String NAME_CREDENTIAL = "credential";
    private static final String ATTR_ID = "id";
    private static final String ATTR_CREDENTIAL_LEVEL = "level";
    private static final String ATTR_CREDENTIAL_TYPE = "type";

    ResourceTable table;
    String resourceId;
    int initialResourceStatus;
    CommCarePlatform instance;
    boolean forceVersion = false;
    int maximumResourceAuthority = -1;

    public ProfileParser(InputStream suiteStream, CommCarePlatform instance, ResourceTable table,
                         String resourceId, int initialResourceStatus, boolean forceVersion)
            throws IOException {

        super(ElementParser.instantiateParser(suiteStream));
        this.table = table;
        this.resourceId = resourceId;
        this.initialResourceStatus = initialResourceStatus;
        this.instance = instance;
        this.forceVersion = forceVersion;
    }

    @Override
    public Profile parse() throws InvalidStructureException, IOException, XmlPullParserException,
            UnfullfilledRequirementsException {

        checkNode("profile");
        Profile profile = parseProfileElement();

        try {
            parser.next();
            int eventType;
            eventType = parser.getEventType();
            do {
                if (eventType == KXmlParser.START_TAG) {
                    if (parser.getName().toLowerCase().equals("property")) {
                        parseProperty(profile);
                    } else if (parser.getName().toLowerCase().equals("root")) {
                        RootTranslator root = new RootParser(this.parser).parse();
                        profile.addRoot(root);
                    } else if (parser.getName().toLowerCase().equals("login")) {
                        parseLogin();
                    } else if (parser.getName().toLowerCase().equals("features")) {
                        parseFeatures(profile);
                    } else if (parser.getName().toLowerCase().equals("suite")) {
                        parseSuite();
                    } else {
                        System.out.println("Unrecognized Tag: "
                                + parser.getName());
                    }
                }
                eventType = parser.next();
            } while (eventType != KXmlParser.END_DOCUMENT);

            return profile;
        } catch (XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new InvalidStructureException("Pull Parse Exception, malformed XML.", parser);
        }
    }

    private Profile parseProfileElement() throws InvalidStructureException, UnfullfilledRequirementsException {
        int version = parseInt(parser.getAttributeValue(null, "version"));
        String authRef = parser.getAttributeValue(null, "update");
        String sMajor = parser.getAttributeValue(null, "requiredMajor");
        String sMinor = parser.getAttributeValue(null, "requiredMinor");
        String sMinimal = parser.getAttributeValue(null, "requiredMinimal");
        String uniqueId = parser.getAttributeValue(null, "uniqueid");
        String displayName = parser.getAttributeValue(null, "name");
        String buildProfileId = parser.getAttributeValue(null, "buildProfileID");

        int major = -1;
        int minor = -1;

        // defaults to 0 since old app builds don't have requiredMinimal defined in Profile
        int minimal = 0;

        if (sMajor != null) {
            major = parseInt(sMajor);
        }
        if (sMinor != null) {
            minor = parseInt(sMinor);
        }
        if (sMinimal != null) {
            minimal = parseInt(sMinimal);
        }

        //If version information is available, check valid versions
        if ((!forceVersion && this.instance != null) && (major != -1) && (minor != -1)) {

            //For the major version, only a matching number is valid, 2.0 cannot be run on either 1.0 or 3.0
            if (this.instance.getMajorVersion() != -1
                    && this.instance.getMajorVersion() != major) {    //changed < to !=
                throw new UnfullfilledRequirementsException(
                        "Major Version Mismatch (Required: " + major + " | Available: " +
                                this.instance.getMajorVersion() + ")",
                        major,
                        minor,
                        minimal,
                        this.instance.getMajorVersion(),
                        this.instance.getMinorVersion(),
                        this.instance.getMinimalVersion(),
                        UnfullfilledRequirementsException.RequirementType.MAJOR_APP_VERSION);
            }

            //For the minor version, anything greater than the profile's version is valid
            if (this.instance.getMinorVersion() != -1
                    && this.instance.getMinorVersion() < minor) {
                throw new UnfullfilledRequirementsException(
                        "Minor Version Mismatch (Required: " + minor + " | Available: " +
                                this.instance.getMinorVersion() + ")",
                        major,
                        minor,
                        minimal,
                        this.instance.getMajorVersion(),
                        this.instance.getMinorVersion(),
                        this.instance.getMinimalVersion(),
                        UnfullfilledRequirementsException.RequirementType.MINOR_APP_VERSION);
            }

            //For the minimal version, anything greater than the profile's version is valid
            if (this.instance.getMinorVersion() == minor &&  this.instance.getMinimalVersion() < minimal) {
                throw new UnfullfilledRequirementsException(
                        "Minimal Version Mismatch (Required: " + minimal + " | Available: " +
                                this.instance.getMinimalVersion() + ")",
                        major,
                        minor,
                        minimal,
                        this.instance.getMajorVersion(),
                        this.instance.getMinorVersion(),
                        this.instance.getMinimalVersion(),
                        UnfullfilledRequirementsException.RequirementType.MINOR_APP_VERSION);
            }
        }

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

        // defaults to empty string instead of null
        if(buildProfileId == null){
            buildProfileId = "";
        }

        return new Profile(version, authRef, uniqueId, displayName, fromOld, buildProfileId);
    }

    private void parseProperty(Profile profile) {
        String key = parser.getAttributeValue(null, "key");
        String value = parser.getAttributeValue(null, "value");
        String force = parser.getAttributeValue(null, "force");
        addPropertySetter(profile, key, value, force);
    }

    private void addPropertySetter(Profile profile, String key, String value, String force) {
        if (force != null) {
            if ("true".equals(force.toLowerCase())) {
                profile.addPropertySetter(key, value, true);
            } else {
                profile.addPropertySetter(key, value, false);
            }
        } else {
            profile.addPropertySetter(key, value);
        }
    }

    private void parseLogin() throws InvalidStructureException, IOException, XmlPullParserException {
        // Get the resource block or fail out
        getNextTagInBlock("login");
        Resource resource = new ResourceParser(parser, maximumResourceAuthority).parse();
        table.addResource(resource, table.getInstallers().getLoginImageInstaller(), resourceId, initialResourceStatus);
    }

    private void parseFeatures(Profile profile) throws XmlPullParserException, IOException, InvalidStructureException {
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
                }
            } else if (tag.equals("package")) {
                //nothing (yet)
            } else if (tag.equals("users")) {
                while (nextTagInBlock("users")) {
                    if (parser.getName().toLowerCase().equals("registration")) {
                        profile.addPropertySetter("user_reg_namespace", parser.nextText(), true);
                    } else if (parser.getName().toLowerCase().equals("logo")) {
                        String logo = parser.nextText();
                        profile.addPropertySetter("cc_login_image", logo, true);
                    } else {
                        throw new InvalidStructureException("Unrecognized tag " + parser.getName() + " inside of users feature block", parser);
                    }
                }
            } else if (tag.equals(NAME_DEPENDENCIES)) {
                profile.setDependencies(parseDependencies());
            } else if (tag.equals("sense")) {
            }else if (tag.equals(NAME_CREDENTIALS)) {
                profile.setCredentials(parseCredentials());
            }

            profile.setFeatureActive(tag, isActive);
            //TODO: set feature activation in profile
        }
    }

    private Vector<Credential> parseCredentials()
            throws InvalidStructureException, XmlPullParserException, IOException {
        Vector<Credential> appCredentials = new Vector<>();
        while (nextTagInBlock(NAME_CREDENTIALS)) {
            String tag = parser.getName().toLowerCase();
            if (tag.equals(NAME_CREDENTIAL)) {
                String level = parser.getAttributeValue(null, ATTR_CREDENTIAL_LEVEL);
                String type = parser.getAttributeValue(null, ATTR_CREDENTIAL_TYPE);
                if (level == null) {
                    throw new InvalidStructureException("No level defined for credential");
                }
                if (type == null) {
                    throw new InvalidStructureException("No type defined for credential");
                }
                appCredentials.add(new Credential(level, type));
            }
        }
        return appCredentials;
    }

    private Vector<AndroidPackageDependency> parseDependencies()
            throws InvalidStructureException, XmlPullParserException, IOException {
        Vector<AndroidPackageDependency> appDependencies = new Vector<>();
        while (nextTagInBlock(NAME_DEPENDENCIES)) {
            String tag = parser.getName().toLowerCase();
            if (tag.equals(NAME_ANDROID_PACKAGE)) {
                String appId = parser.getAttributeValue(null, ATTR_ID);
                if (appId == null) {
                    throw new InvalidStructureException("No id defined for app dependency");
                }
                appDependencies.add(new AndroidPackageDependency(appId));
            }
        }
        return appDependencies;
    }

    private void parseSuite() throws InvalidStructureException, XmlPullParserException, IOException {
        // Get the resource block or fail out
        getNextTagInBlock("suite");
        Resource resource = new ResourceParser(parser, maximumResourceAuthority).parse();
        //TODO: Possibly add a real parent reference if we decide these go in the table
        table.addResource(resource, table.getInstallers().getSuiteInstaller(), resourceId, initialResourceStatus);
    }

    public void setMaximumAuthority(int authority) {
        maximumResourceAuthority = authority;
    }

}
