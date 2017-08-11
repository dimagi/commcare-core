package org.commcare.test.utilities;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.parse.CommCareTransactionParserFactory;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.core.parse.ParseUtils;
import org.commcare.util.engine.CommCareConfigEngine;
import org.commcare.util.mocks.*;
import org.commcare.util.mocks.JavaPlatformSyncTool;
import org.commcare.util.mocks.SyncStateMachine;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.User;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.form.api.FormEntryController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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

    CoreNetworkContext context;

    /**
     * Creates and initializes a mockapp that is located at the provided Java Resource path.
     *
     * @param resourcePath The resource path to a an app template. Needs to contain a leading and
     *                     trailing slash, like /path/app/
     */
    public MockApp(String resourcePath) throws Exception {
        this(resourcePath, "user_restore.xml", null);
    }

    /**
     * Creates and initializes a mockapp that is located at the provided Java Resource path.
     *
     * @param resourcePath The resource path to a an app template. Needs to contain a leading and
     *                     trailing slash, like /path/app/
     * @param userRestorePath The path to a user restore relative to the app template resource path.
     *                        Does not require a leading slash
     */
    public MockApp(String resourcePath, String userRestorePath, LivePrototypeFactory factory) throws Exception {
        if(!(resourcePath.startsWith("/") && resourcePath.endsWith("/"))) {
            throw new IllegalArgumentException("Invalid resource path for a mock app " + resourcePath);
        }
        APP_BASE = resourcePath;
        final LivePrototypeFactory mPrototypeFactory = factory == null? setupStaticStorage() : factory;
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

        if(userRestorePath != null) {
            ParseUtils.parseIntoSandbox(this.getClass().getResourceAsStream(APP_BASE + userRestorePath), mSandbox);

            //If we parsed in a user, arbitrarily log one in.
            mSandbox.setLoggedInUser(mSandbox.getUserStorage().read(0));
        }

        mSessionWrapper = new SessionWrapper(mEngine.getPlatform(), mSandbox);
    }

    public void performInitialUserRestoreFromNetwork(String username, String password) {
        context = new CoreNetworkContext(username, password);

        syncData();

        UserSandbox sandbox = mSessionWrapper.getSandbox();

        //Initialize our User
        for (IStorageIterator<User> iterator = sandbox.getUserStorage().iterate(); iterator.hasMore(); ) {
            User u = iterator.nextRecord();
            if (username.equalsIgnoreCase(u.getUsername())) {
                sandbox.setLoggedInUser(u);
            }
        }

        if (mSessionWrapper != null) {
            // old session data is now no longer valid
            mSessionWrapper.clearVolitiles();
        }

    }

    public JavaPlatformSyncTool getSyncTool() {
        UserSandbox sandbox = mSessionWrapper.getSandbox();
        return new org.commcare.util.mocks.JavaPlatformSyncTool(context.getPlainUsername(), context.getPassword(),
                        sandbox, mSessionWrapper);

    }

    public void syncData() {
        org.commcare.util.mocks.JavaPlatformSyncTool tool = getSyncTool();

        attemptSync(tool);

    }

    private static void attemptSync(JavaPlatformSyncTool tool) {
        try {
            tool.initialize();

            while (tool.getCurrentState() == org.commcare.util.mocks.SyncStateMachine.State.Ready_For_Request) {

                tool.performRequest();

                switch (tool.getCurrentState()) {
                    case Waiting_For_Progress:
                        tool.processWaitSignal();
                        break;
                    case Recovery_Requested:
                        tool.transitionToRecoveryStrategy();
                        break;
                    case Recoverable_Error:
                        tool.resetFromError();
                        break;
                }
            }

            tool.processPayload();
        } catch(org.commcare.util.mocks.SyncStateMachine.SyncErrorException e) {
            throw new RuntimeException("Couldn't sync initial user data");
        }
    }

    public void processForm(byte[] incomingForm) {
        ByteArrayInputStream bais = new ByteArrayInputStream(incomingForm);
        try {
            DataModelPullParser parser = new DataModelPullParser(
                    bais, new CommCareTransactionParserFactory(mSessionWrapper.getSandbox()), true, true);
            parser.parse();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                bais.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void submitForm(byte[] incomingForm) {
        JavaPlatformFormSubmitTool submitter = new JavaPlatformFormSubmitTool(mSessionWrapper.getSandbox(), context);
        if(!submitter.submitFormToServer(incomingForm)) {
            throw new RuntimeException("Error submitting form during tests");
        }
    }


    /**
     * Loads the provided form and properly initializes external data instances,
     * such as the casedb and commcare session.
     */
    public FormEntryController loadAndInitForm(String formFileInApp) {
        FormParseInit fpi = new FormParseInit(APP_BASE + formFileInApp);
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        FormDef fd = fpi.getFormDef();
        // run initialization to ensure xforms-ready event and binds are
        // triggered.
        fd.initialize(true, mSessionWrapper.getIIF());
        return fec;
    }


    private static LivePrototypeFactory setupStaticStorage() {
        return new LivePrototypeFactory();
    }

    public SessionWrapper getSession() {
        return mSessionWrapper;
    }

}
