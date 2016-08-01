package org.javarosa.core.model.test;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.ResourceReferenceFactory;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.model.xform.XPathReference;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.fail;

public class QuestionDefTest {
    QuestionDef q = null;
    FormEntryPrompt fep = null;
    FormParseInit fpi = null;

    @Before
    public void initStuff() {
        fpi = new FormParseInit("/ImageSelectTester.xhtml");
        q = fpi.getFirstQuestionDef();
        fep = new FormEntryPrompt(fpi.getFormDef(), fpi.getFormEntryModel().getFormIndex());
    }

    static final PrototypeFactory pf;

    static {
        PrototypeManager.registerPrototype("org.javarosa.model.xform.XPathReference");
        pf = ExtUtil.defaultPrototypes();
    }

    private void testSerialize(QuestionDef q, String msg) {
        //ExternalizableTest.testExternalizable(q, this, pf, "QuestionDef [" + msg + "]");
    }

    @Test
    public void testConstructors() {
        QuestionDef q;

        q = new QuestionDef();
        if (q.getID() != -1) {
            fail("QuestionDef not initialized properly (default constructor)");
        }
        testSerialize(q, "a");

        q = new QuestionDef(17, Constants.CONTROL_RANGE);
        if (q.getID() != 17) {
            fail("QuestionDef not initialized properly");
        }
        testSerialize(q, "b");
    }

    public XPathReference newRef(String xpath) {
        pf.addClass(XPathReference.class);
        return new XPathReference(xpath);
    }

    @Test
    public void testAccessorsModifiers() {
        QuestionDef q = new QuestionDef();

        q.setID(45);
        if (q.getID() != 45) {
            fail("ID getter/setter broken");
        }
        testSerialize(q, "c");

        XPathReference ref = newRef("/data");
        q.setBind(ref);
        if (q.getBind() != ref) {
            fail("Ref getter/setter broken");
        }
        testSerialize(q, "e");

        q.setControlType(Constants.CONTROL_SELECT_ONE);
        if (q.getControlType() != Constants.CONTROL_SELECT_ONE) {
            fail("Control type getter/setter broken");
        }
        testSerialize(q, "g");

        q.setAppearanceAttr("minimal");
        if (!"minimal".equals(q.getAppearanceAttr())) {
            fail("Appearance getter/setter broken");
        }
        testSerialize(q, "h");
    }

    @Test
    public void testChild() {
        QuestionDef q = new QuestionDef();

        if (q.getChildren() != null) {
            fail("Question has children");
        }

        try {
            q.setChildren(new Vector());
            fail("Set a question's children without exception");
        } catch (IllegalStateException ise) {
            //expected
        }

        try {
            q.addChild(new QuestionDef());
            fail("Added a child to a question without exception");
        } catch (IllegalStateException ise) {
            //expected
        }
    }

    @Test
    public void testReferences() {
        QuestionDef q = fpi.getFirstQuestionDef();
        FormEntryPrompt fep = fpi.getFormEntryModel().getQuestionPrompt();

        Localizer l = fpi.getFormDef().getLocalizer();
        l.setDefaultLocale(l.getAvailableLocales()[0]);
        l.setLocale(l.getAvailableLocales()[0]);

        String audioURI = fep.getAudioText();
        String ref;

        ReferenceManager._().addReferenceFactory(new ResourceReferenceFactory());
        ReferenceManager._().addRootTranslator(new RootTranslator("jr://audio/", "jr://resource/"));
        try {
            Reference r = ReferenceManager._().DeriveReference(audioURI);
            ref = r.getURI();
            if (!ref.equals("jr://resource/hah.mp3")) {
                fail("Root translation failed.");
            }
        } catch (InvalidReferenceException ire) {
            fail("There was an Invalid Reference Exception:" + ire.getMessage());
            ire.printStackTrace();
        }


        ReferenceManager._().addRootTranslator(new RootTranslator("jr://images/", "jr://resource/"));
        q = fpi.getNextQuestion();
        fep = fpi.getFormEntryModel().getQuestionPrompt();
        String imURI = fep.getImageText();
        try {
            Reference r = ReferenceManager._().DeriveReference(imURI);
            ref = r.getURI();
            if (!ref.equals("jr://resource/four.gif")) {
                fail("Root translation failed.");
            }
        } catch (InvalidReferenceException ire) {
            fail("There was an Invalid Reference Exception:" + ire.getMessage());
            ire.printStackTrace();
        }
    }

    public QuestionDef getQ() {
        return q;
    }

    public void setQ(QuestionDef q) {
        this.q = q;
    }
}
