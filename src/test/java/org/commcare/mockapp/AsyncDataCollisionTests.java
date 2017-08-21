package org.commcare.mockapp;

import org.commcare.backend.suite.model.test.EmptyAppElementsTests;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionNavigator;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.MenuLoader;
import org.commcare.test.utilities.MockApp;
import org.commcare.test.utilities.MockSessionNavigationResponder;
import org.commcare.test.utilities.XFormTestUtilities;
import org.commcare.util.LoggerInterface;
import org.commcare.util.mocks.JavaPlatformFormSubmitTool;
import org.commcare.util.mocks.JavaPlatformSyncTool;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.Normalizer;

import static org.junit.Assert.assertEquals;

/**
 * Created by ctsims on 8/10/2017.
 */

public class AsyncDataCollisionTests {

    private MockApp mockAppUser1;
    private MockApp mockAppUser2;

    private SessionNavigator app1SessionNavigator;
    private SessionNavigator app2SessionNavigator;


    private static final String TEST_CASE_ID = "8a612ef5-7388-4e16-821c-3768bb074132";

    @Before
    public void setUp() throws Exception {
        LivePrototypeFactory sharedFactory = new LivePrototypeFactory();

        mockAppUser1 = new MockApp("/app_data_collision/", null, sharedFactory);
        mockAppUser1.performInitialUserRestoreFromNetwork("test.sync.a","123");

        MockSessionNavigationResponder mockSessionNavigationResponder =
                new MockSessionNavigationResponder(mockAppUser1.getSession());
        app1SessionNavigator = new SessionNavigator(mockSessionNavigationResponder);


        mockAppUser2 = new MockApp("/app_data_collision/", null, sharedFactory);
        mockAppUser2.performInitialUserRestoreFromNetwork("test.sync.b", "123");

        MockSessionNavigationResponder mockSessionNavigationResponder2 =
                new MockSessionNavigationResponder(mockAppUser2.getSession());
        app2SessionNavigator = new SessionNavigator(mockSessionNavigationResponder2);

    }

    @Test
    public void testDataCollision() {
        setValueToNumber(app1SessionNavigator, mockAppUser1, 0);

        mockAppUser2.syncData();

        ensureDataIsSetToNumber(app2SessionNavigator, mockAppUser2,0);

        setValueToNumber(app1SessionNavigator, mockAppUser1, 1);

        mockAppUser2.syncData();

        setValueToNumber(app2SessionNavigator, mockAppUser2, 2);

        triggerSyncRequire202ThenNeverComplete(mockAppUser1.getSyncTool());

        setValueToNumber(app1SessionNavigator, mockAppUser1, 5);

        mockAppUser1.syncData();

        ensureDataIsSetToNumber(app1SessionNavigator, mockAppUser1,5);

    }

    private void triggerSyncRequire202ThenNeverComplete(JavaPlatformSyncTool tool) {
        try {
            tool.initialize();

            while (tool.getCurrentState() == org.commcare.util.mocks.SyncStateMachine.State.Ready_For_Request) {
                tool.performRequest();

                switch (tool.getCurrentState()) {
                    case Waiting_For_Progress:
                        return;
                    case Recovery_Requested:
                        throw new RuntimeException("Unexpected State during tests");
                    case Recoverable_Error:
                        tool.resetFromError();
                        break;
                }
            }
            throw new RuntimeException("Test failed due to lack of asynchronous sync response");

        } catch(org.commcare.util.mocks.SyncStateMachine.SyncErrorException e) {
            throw new RuntimeException("Error Syncing during tests");
        }

    }

    private void ensureDataIsSetToNumber(SessionNavigator navigator, MockApp app, int value) {
        FormEntryController fec = navigateSessionToForm(navigator, app);
        SessionWrapper session = app.getSession();

        fec.stepToNextEvent();

        FormEntryPrompt prompt = fec.getQuestionPrompts()[0];
        Assert.assertEquals(String.format("The current value is:%d",value), prompt.getQuestionText());

        session.clearAllState();
        session.clearVolitiles();
    }

    private void setValueToNumber(SessionNavigator navigator, MockApp app, int value) {
        FormEntryController fec = navigateSessionToForm(navigator, app);
        SessionWrapper session = app.getSession();

        fec.stepToNextEvent();

        fec.stepToNextEvent();

        fec.answerQuestion(new IntegerData(value));

        byte[] form = XFormTestUtilities.finalizeAndSerializeForm(fec);

        app.processForm(form);

        app.submitForm(form);

        session.clearAllState();
        session.clearVolitiles();

    }

    private FormEntryController navigateSessionToForm(SessionNavigator navigator, MockApp app) {
        SessionWrapper session = app.getSession();

        navigator.startNextSessionStep();

        session.setCommand("m1");

        navigator.startNextSessionStep();

        session.setDatum("case_id", TEST_CASE_ID);
        navigator.startNextSessionStep();
        session.setCommand("m0-f0");
        navigator.startNextSessionStep();

        FormEntryController fec = app.loadAndInitForm("modules-1/forms-0.xml");

        assertEquals(FormEntryController.EVENT_BEGINNING_OF_FORM, fec.getModel().getEvent());

        return fec;
    }
}
