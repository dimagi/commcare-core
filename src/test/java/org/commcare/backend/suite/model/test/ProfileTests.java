package org.commcare.backend.suite.model.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.suite.model.AndroidPackageDependency;
import org.commcare.suite.model.Credential;
import org.commcare.suite.model.Profile;
import org.commcare.test.utilities.PersistableSandbox;
import org.commcare.util.engine.CommCareConfigEngine;
import org.commcare.xml.ProfileParser;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.ArrayUtilities;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;

/**
 * Regressions and unit tests for the profile model.
 * 
 * Covers functioning specific to the profile itself, not
 * to setup/usage of resource tables or other models.
 * 
 * @author ctsims
 */
public class ProfileTests {
    private PersistableSandbox mSandbox;
    private CommCareConfigEngine mAppPlatform;
    private ResourceTable mFreshResourceTable;

    private final static String BASIC_PROFILE_PATH = "/basic_profile.ccpr";
    private final static String MULT_APPS_PROFILE_PATH = "/multiple_apps_profile.ccpr";


    @Before
    public void setUp() {
        mSandbox = new PersistableSandbox();
        mAppPlatform = new CommCareConfigEngine();
        mFreshResourceTable = ResourceTable.RetrieveTable(
                new DummyIndexedStorageUtility(Resource.class, new LivePrototypeFactory()));
    }
    
    @Test
    public void testProfileParse() {
        Profile p = getProfile(BASIC_PROFILE_PATH);
        assertEquals("Profile is not set to the correct version: (102)", p.getVersion(), 102);
    }
    
    @Test
    public void testBasicProfileSerialization() {
        Profile p = getProfile(BASIC_PROFILE_PATH);
        byte[] serializedProfile = mSandbox.serialize(p);
        
        Profile deserialized = mSandbox.deserialize(serializedProfile, Profile.class);
        
        //Maybe this should just be p.equals(deserialized)? Kind of hard to say with deep
        //models of this type.
        compareProfiles(p, deserialized);
    }

    @Test
    public void testMultipleAppsProfileSerialization() {
        Profile p = getProfile(MULT_APPS_PROFILE_PATH);
        byte[] serializedProfile = mSandbox.serialize(p);
        Profile deserialized = mSandbox.deserialize(serializedProfile, Profile.class);
        compareProfiles(p, deserialized);
    }

    // Tests that a profile.ccpr which was missing the necessary fields for multiple apps has
    // them generated correctly by the parser
    @Test
    public void testGeneratedProfileFields() {
        Profile p = getProfile(BASIC_PROFILE_PATH);
        assertNotNull("Profile uniqueId was null", p.getUniqueId());
        assertNotNull("Profile display name was null", p.getDisplayName());
    }

    @Test
    public void testDependenciesParse() {
        Profile p = getProfile(BASIC_PROFILE_PATH);
        assertTrue(p.isFeatureActive("dependencies"));
        AndroidPackageDependency[] expectedDependencies = new AndroidPackageDependency[2];
        expectedDependencies[0] = new AndroidPackageDependency("org.commcare.reminders");
        expectedDependencies[1] = new AndroidPackageDependency("org.commcare.test");
        assertEquals(Arrays.toString(expectedDependencies),Arrays.toString(p.getDependencies().toArray()));
    }

    @Test
    public void testCredentialsParse() {
        Profile p = getProfile(BASIC_PROFILE_PATH);
        assertTrue(p.isFeatureActive("credentials"));
        Credential[] expectedCredentials = new Credential[2];
        expectedCredentials[0] = new Credential("3MON_ACTIVE", "APP_ACTIVITY");
        expectedCredentials[1] = new Credential("6MON_ACTIVE", "APP_ACTIVITY");
        assertEquals(Arrays.toString(expectedCredentials),Arrays.toString(p.getCredentials().toArray()));
    }

    private void compareProfiles(Profile a, Profile b) {
        if(!ArrayUtilities.arraysEqual(a.getPropertySetters(), b.getPropertySetters())) {
            fail("Mismatch of property setters between profiles");
        }

        assertEquals("Mismatched auth references", a.getAuthReference(), b.getAuthReference());
        assertEquals("Mismatched profile versions", a.getVersion(), b.getVersion());
        assertEquals("Mismatched profile unique ids", a.getUniqueId(), b.getUniqueId());
        assertEquals("Mismatched profile display names", a.getDisplayName(), b.getDisplayName());
        assertEquals("Mismatched profiles on old version", a.isOldVersion(), b.isOldVersion());

        //TODO: compare root references and other mismatched fields
    }

    private Profile getProfile(String path) {
        try{
            InputStream is = this.getClass().getResourceAsStream(path);

            if(is == null) {
                throw new RuntimeException("Test resource missing: " + path);
            }
            
            ProfileParser parser = new ProfileParser(is, mAppPlatform.getPlatform(), mFreshResourceTable, "profile",
                    Resource.RESOURCE_VERSION_UNKNOWN, false);
            
            return parser.parse();
        } catch(Exception e) {
            e.printStackTrace();
            throw PersistableSandbox.wrap("Error during profile test setup", e);
        }
    }

}
