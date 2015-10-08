package org.commcare.api;

import org.commcare.api.transform.XmlUtils;
import org.commcare.cases.model.Case;
import org.commcare.core.parse.ParseUtils;
import org.commcare.test.utilities.TestInstanceInitializer;
import org.commcare.util.mocks.LivePrototypeFactory;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

/**
 * Created by wpride1 on 9/18/15.
 */
public class TransformTest {
    private final static String BASIC_RESTORE_PATH = "/simple_restore.xml";
    private final static String BASIC_FORM_PATH = "/basic_form.xml";
    private final static String user = "test";
    private final static String pass = "123";

    MockUserDataSandbox mSandbox;
    @Before
    public void setUp() {
        PrototypeFactory mPrototypeFactory = setupStaticStorage();
        mSandbox =  new MockUserDataSandbox(mPrototypeFactory);
        try {
            ParseUtils.parseIntoSandbox(this.getClass().getResourceAsStream(BASIC_RESTORE_PATH), mSandbox);
        } catch (InvalidStructureException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTransform() {
        IStorageUtilityIndexed<Case> caseStorage = mSandbox.getCaseStorage();
        int count = caseStorage.getNumRecords();

        XmlUtils.printExternalInstance("jr://instance/casedb", new TestInstanceInitializer(mSandbox));

        IStorageIterator mIterator = caseStorage.iterate();
        Vector<Case> caseAccumulator = new Vector<Case>();
        while(mIterator.hasMore()){
            //caseAccumulator.add(mIterator.nextRecord());
        }
        String xml = XmlUtils.getCaseXml(caseAccumulator);
    }



    // from MockApp, put into util
    private static LivePrototypeFactory setupStaticStorage() {
        LivePrototypeFactory prototypeFactory = new LivePrototypeFactory();
        return prototypeFactory;
    }
}
