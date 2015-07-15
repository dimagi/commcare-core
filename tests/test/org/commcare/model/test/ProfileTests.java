package org.commcare.model.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.suite.model.Profile;
import org.commcare.test.utils.PersistableSandbox;
import org.commcare.util.CommCareConfigEngine;
import org.commcare.xml.ProfileParser;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.ArrayUtilities;
import org.junit.Before;
import org.junit.Test;

/**
 * Regressions and unit tests for the profile model.
 * 
 * Covers functioning specific to the profile itself, not
 * to setup/usage of resource tables or other models.
 * 
 * @author ctsims
 */
public class ProfileTests {
    PersistableSandbox mSandbox;
    CommCareConfigEngine mAppPlatform;
    ResourceTable mFreshResourceTable;
    
    @Before
    public void setUp() {
        mSandbox = new PersistableSandbox();
        mAppPlatform = new CommCareConfigEngine();
        mFreshResourceTable = ResourceTable.RetrieveTable(
                new DummyIndexedStorageUtility(ResourceTable.class));
    }
    
    @Test
    public void testProfileParse() {
        Profile p = getBasicProfile();
        assertEquals("Profile is not set to the correct version: (102)", p.getVersion(), 102);
    }
    
    @Test
    public void testProfileSerialization() {
        Profile p = getBasicProfile();
        byte[] serializedProfile = mSandbox.serialize(p);
        
        Profile deserialized = mSandbox.deserialize(serializedProfile, Profile.class);
        
        //Maybe this should just be p.equals(deserialized)? Kind of hard to say with deep
        //models of this type.
        compareProfiles(p, deserialized);
    }
    
    public void compareProfiles(Profile a, Profile b) {
        if(!ArrayUtilities.arraysEqual(a.getPropertySetters(), b.getPropertySetters())) {
            fail("Mismatch of property setters between profiles");
        }
        
        assertEquals("Mismatched auth references", a.getAuthReference(), b.getAuthReference());

        assertEquals("Mismatched profile versions", a.getVersion(), b.getVersion());
        
        //TOOD: compare root references and other mismatched fields
    }

    private Profile getBasicProfile() {
        try{
            String basicProfilePath = "/basic_profile.ccpr";
            InputStream is = this.getClass().getResourceAsStream(basicProfilePath);
            if(is == null) {
                throw new RuntimeException("Test resource missing: " + basicProfilePath);
            }
            
            ProfileParser parser = new ProfileParser(is, mAppPlatform.getInstance(), mFreshResourceTable, "profile", 
                    Resource.RESOURCE_VERSION_UNKNOWN, false);
            
            Profile p = parser.parse();
            return p;
        } catch(Exception e) {
            throw PersistableSandbox.wrap("Error during profile test setup", e);
        }
    }
}
