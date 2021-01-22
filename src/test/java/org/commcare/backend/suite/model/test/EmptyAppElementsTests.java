package org.commcare.backend.suite.model.test;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.Global;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.MenuLoader;
import org.commcare.test.utilities.MockApp;
import org.commcare.util.LoggerInterface;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for app functioning with empty (edge case) elements
 *
 * @author ctsims
 */
public class EmptyAppElementsTests {

    private MockApp mApp;
    private MenuDisplayable[] mChoices;

    @Before
    public void setUp() throws Exception {
        mApp = new MockApp("/empty_app_elements/");
    }

    @Test
    public void testEmptyMenu() {
        SessionWrapper session = mApp.getSession();

        MenuLoader menuLoader = new MenuLoader(session.getPlatform(), session, "root", new TestLogger(), false, false);
        this.mChoices = menuLoader.getMenus();
        Assert.assertEquals("Number of Menu roots in empty example", this.mChoices.length, 1);
    }

    @Test
    public void testEmptyGlobal() {
        Global global = mApp.getSession().getPlatform().getDetail("m0_case_short").getGlobal();
        Assert.assertEquals(0, global.getGeoOverlays().length);
    }

    public static class TestLogger implements LoggerInterface {

        @Override
        public void logError(String message, Exception cause) {
            Assert.fail(message);
        }

        @Override
        public void logError(String message) {
            Assert.fail(message);
        }
    }
}
