package org.commcare.test.utilities;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.core.parse.ParseUtils;
import org.commcare.util.engine.CommCareConfigEngine;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.form.api.FormEntryController;

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
    private final SessionWrapper mSessionWrapper;
    private final String APP_BASE;

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
        APP_BASE = resourcePath;
        final LivePrototypeFactory mPrototypeFactory = setupStaticStorage();
        CommCareConfigEngine.setStorageFactory(new IStorageIndexedFactory() {
            @Override
            public IStorageUtilityIndexed newStorage(String name, Class type) {
                return new DummyIndexedStorageUtility(type, mPrototypeFactory);
            }
        });
        MockUserDataSandbox mSandbox = new MockUserDataSandbox(mPrototypeFactory);
        CommCareConfigEngine mEngine = new CommCareConfigEngine(mPrototypeFactory);

        mEngine.installAppFromReference("jr://resource" + APP_BASE + "profile.ccpr");
        mEngine.initEnvironment();
        ParseUtils.parseIntoSandbox(this.getClass().getResourceAsStream(APP_BASE + "user_restore.xml"), mSandbox);

        //If we parsed in a user, arbitrarily log one in.
        mSandbox.setLoggedInUser(mSandbox.getUserStorage().read(0));

        mSessionWrapper = new SessionWrapper(mEngine.getPlatform(), mSandbox);
    }

    /**
     * Loads the provided form and properly initializes external data instances,
     * such as the casedb and commcare session.
     */
    public FormParseInit loadAndInitForm(String formFileInApp) {
        FormParseInit fpi = new FormParseInit(APP_BASE + formFileInApp);
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        FormDef fd = fpi.getFormDef();
        // run initialization to ensure xforms-ready event and binds are
        // triggered.
        fd.initialize(true, mSessionWrapper.getIIF());
        return fpi;
    }


    private static LivePrototypeFactory setupStaticStorage() {
        return new LivePrototypeFactory();
    }

    public SessionWrapper getSession() {
        return mSessionWrapper;
    }

}
