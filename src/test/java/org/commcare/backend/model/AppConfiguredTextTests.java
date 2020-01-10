package org.commcare.backend.model;

import org.commcare.backend.session.test.SessionStackTests;
import org.commcare.backend.suite.model.test.EmptyAppElementsTests;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.MenuLoader;
import org.commcare.test.utilities.MockApp;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.services.locale.Localization;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests navigating through a CommCareSession (setting datum values and commands, using stepBack(),
 * etc.) for a sample app
 *
 * @author amstone
 */
public class AppConfiguredTextTests {

    private MockApp mApp;
    private SessionWrapper session;

    @Before
    public void setUp() throws Exception {
        mApp = new MockApp("/app_for_text_tests/");
        session = mApp.getSession();
        Localization.setDefaultLocale("default");
    }

    @Test
    public void testBasicText() {

        MenuDisplayable display = getDisplayable("test1");

        EvaluationContext evaluationContext = session.getEvaluationContext("test1");

        Assert.assertEquals("RAWTEXT", display.getDisplayText(evaluationContext));
    }

    @Test
    public void testLocalizedTextBehavior() {
        Localization.setLocale("en");
        MenuDisplayable display = getDisplayable("test2");
        EvaluationContext evaluationContext = session.getEvaluationContext("test2");

        Assert.assertEquals("EnglishString", display.getDisplayText(evaluationContext));

        Localization.setLocale("hin");

        Assert.assertEquals("DefaultString", display.getDisplayText(evaluationContext));
    }

    @Test
    public void testLocalizationParams() {
        Localization.setLocale("en");
        MenuDisplayable display = getDisplayable("test3");
        EvaluationContext evaluationContext = session.getEvaluationContext("test3");

        Assert.assertEquals("ValueArgument", display.getDisplayText(evaluationContext));

        Localization.setLocale("hin");

        Assert.assertEquals("ArgumentValue", display.getDisplayText(evaluationContext));

    }

    @Test
    public void testLocaliationIdParam() {
        Localization.setLocale("en");
        MenuDisplayable display = getDisplayable("test4");
        EvaluationContext evaluationContext = session.getEvaluationContext("test4");

        Assert.assertEquals("Message1", display.getDisplayText(evaluationContext));

        Localization.setLocale("hin");

        Assert.assertEquals("Message2", display.getDisplayText(evaluationContext));

    }

    @Test
    public void testMultipleArgs() {
        Localization.setLocale("en");
        MenuDisplayable display = getDisplayable("test5");
        EvaluationContext evaluationContext = session.getEvaluationContext("test5");

        Assert.assertEquals("OneThreeTwo", display.getDisplayText(evaluationContext));

        Localization.setLocale("hin");

        Assert.assertEquals("TwoOneThree", display.getDisplayText(evaluationContext));

    }
    @Test
    public void testMultipleArgsAndId() {
        Localization.setLocale("en");
        MenuDisplayable display = getDisplayable("test6");
        EvaluationContext evaluationContext = session.getEvaluationContext("test6");

        Assert.assertEquals("OneThreeTwo", display.getDisplayText(evaluationContext));

        Localization.setLocale("hin");

        Assert.assertEquals("TwoOneThree", display.getDisplayText(evaluationContext));

    }

    private MenuDisplayable getDisplayable(String commandId) {
        MenuLoader menuLoader = new MenuLoader(session.getPlatform(), session, "root",
                new EmptyAppElementsTests.TestLogger(), false, false);

        for(MenuDisplayable displayable : menuLoader.getMenus()) {
            if(displayable.getCommandID().equals(commandId)) {
                return displayable;
            }
        }
        throw new RuntimeException("No Command " + commandId + " found in test harness");
    }


}
