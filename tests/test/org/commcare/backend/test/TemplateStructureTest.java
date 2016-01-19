package org.commcare.backend.test;

import org.commcare.api.session.SessionWrapper;
import org.commcare.test.utilities.MockApp;
import org.junit.Assert;

import org.commcare.session.SessionFrame;
import org.junit.Before;
import org.junit.Test;

/**
 * This is a super basic test just to make sure the test infrastructure is working correctly
 * and to act as an example of how to build template app tests.
 *
 * Created by ctsims on 8/14/2015.
 */
public class TemplateStructureTest {
    MockApp mApp;

    @Before
    public void init() throws Exception{
        mApp = new MockApp("/template/");
    }

    @Test
    public void testBasicSessionWalk() {
        SessionWrapper session = mApp.getSession();
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);

        session.setCommand("m0");

        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_DATUM_VAL);
        Assert.assertEquals(session.getNeededDatum().getDataId(), "case_id");

        session.setDatum("case_id", "case_one");

        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);

        session.setCommand("m0-f0");

        Assert.assertEquals(session.getNeededData(), null);

        Assert.assertEquals(session.getForm(), "http://commcarehq.org/test/placeholder");
    }

}
