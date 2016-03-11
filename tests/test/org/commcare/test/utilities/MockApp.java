package org.commcare.test.utilities;

import org.commcare.api.session.SessionWrapper;
import org.commcare.core.parse.ParseUtils;
import org.commcare.util.CommCareConfigEngine;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.User;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;

/**
 * A mock app is a quick test wrapper that makes it easy to start playing with a live instance
 * of a CommCare app including the session and user data that goes along with it.
 *
 * To use this class, make a copy of the /template/ app in the test resources and extend the
 * config and data in that copy, then pass its resource path to the constructor.
 *
 * Created by ctsims on 8/14/2015.
 */
public class MockApp {
    private final MockUserDataSandbox mSandbox;
    private final LivePrototypeFactory mPrototypeFactory;
    private final CommCareConfigEngine mEngine;
    private final SessionWrapper mSessionWrapper;

    /**
     * Creates and initializes a mockapp that is located at the provided Java Resource path.
     *
     * @param resourcePath The resource path to a an app template. Needs to contain a leading and
     *                     trailing slash, like /path/app/
     */
    public MockApp(String resourcePath) throws Exception {
        if(!(resourcePath.startsWith("/") && resourcePath.endsWith("/"))) {
            throw new IllegalArgumentException("Invalid resource path for a mock app " + resourcePath);
        }
        mPrototypeFactory = setupStaticStorage();
        mSandbox =  new MockUserDataSandbox(mPrototypeFactory);
        mEngine = new CommCareConfigEngine(mPrototypeFactory);

        mEngine.installAppFromReference("jr://resource" + resourcePath + "profile.ccpr");
        mEngine.initEnvironment();
        ParseUtils.parseIntoSandbox(this.getClass().getResourceAsStream(resourcePath + "user_restore.xml"), mSandbox);

        //If we parsed in a user, arbitrarily log one in.
        for(IStorageIterator<User> iterator = mSandbox.getUserStorage().iterate(); iterator.hasMore();) {
            mSandbox.setLoggedInUser(iterator.nextRecord());
            break;
        }

        mSessionWrapper = new SessionWrapper(mEngine.getPlatform(), mSandbox);
    }


    private static LivePrototypeFactory setupStaticStorage() {
        return new LivePrototypeFactory();
    }

    public SessionWrapper getSession() {
        return mSessionWrapper;
    }

}
