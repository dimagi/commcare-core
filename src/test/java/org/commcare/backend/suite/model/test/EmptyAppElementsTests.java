package org.commcare.backend.suite.model.test;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Callout;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.MenuLoader;
import org.commcare.suite.model.OfflineUserRestore;
import org.commcare.suite.model.Suite;
import org.commcare.suite.model.Text;
import org.commcare.test.utilities.MockApp;
import org.commcare.util.LoggerInterface;
import org.commcare.util.screen.MenuScreen;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.services.Logger;
import org.javarosa.xpath.expr.XPathExpression;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

        MenuLoader menuLoader = new MenuLoader(session.getPlatform(), session, "root", new TestLogger());
        this.mChoices = menuLoader.getMenus();
        Assert.assertEquals("Number of Menu roots in empty example", this.mChoices.length, 1);
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
