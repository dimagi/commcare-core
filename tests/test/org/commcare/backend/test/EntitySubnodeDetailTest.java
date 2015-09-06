package org.commcare.backend.test;

import org.commcare.suite.model.Detail;
import org.commcare.test.utilities.MockApp;
import org.commcare.util.mocks.SessionWrapper;
import org.javarosa.core.model.instance.TreeReference;
import org.junit.Assert;

import org.commcare.util.SessionFrame;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * This tests the functionality of detail screens that display fields for  entity subnodes.
 *
 * Created by jschweers on 9/2/2015.
 */
public class EntitySubnodeDetailTest {
    MockApp mApp;

    @Before
    public void init() throws Exception{
        mApp = new MockApp("/entity_subnode_detail/");
    }

    @Test
    public void testDetailDisplay() {
        SessionWrapper session = mApp.getSession();
        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_COMMAND_ID);

        session.setCommand("m0");

        Assert.assertEquals(session.getNeededData(), SessionFrame.STATE_DATUM_VAL);
        Assert.assertEquals(session.getNeededDatum().getDataId(), "report_id_my_report");
        Assert.assertEquals(session.getNeededDatum().getLongDetail(), "reports.my_report.data");

        Detail confirmDetail = session.getDetail(session.getNeededDatum().getLongDetail());
        Assert.assertNotNull(confirmDetail.getNodeset());

        TreeReference detailReference = session.getNeededDatum().getNodeset();
        detailReference = confirmDetail.getNodeset().contextualize(detailReference);
        List<TreeReference> references = session.getEvaluationContext().expandReference(detailReference);
        Assert.assertEquals(4, references.size());
    }

}
