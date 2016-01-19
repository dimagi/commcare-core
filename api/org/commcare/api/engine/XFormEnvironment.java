/**
 *
 */
package org.commcare.api.engine;

import org.commcare.api.engine.models.Action;
import org.commcare.api.engine.models.Mockup;
import org.commcare.api.engine.models.Session;
import org.commcare.api.engine.models.Step;
import org.commcare.api.session.SessionWrapper;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;

import java.util.Date;
import java.util.Vector;

/**
 *
 * Run an XForm programatically for fun and profit.
 *
 * @author ctsims
 *
 */
public class XFormEnvironment {

    private FormDef form;

    private FormEntryModel fem;
    private FormEntryController fec;


    private Step currentStep;
    private int stepCount = 0;

    private Session session;
    private Mockup mockup;
    boolean recording = true;
    private SessionWrapper sessionWrapper;

    public XFormEnvironment(FormDef form) {
        this.form = form;
    }

    public XFormEnvironment(FormDef form, Session session, SessionWrapper sessionWrapper) {
        this.form = form;
        this.session = session;
        this.sessionWrapper = sessionWrapper;
        recording = false;
    }


    public XFormEnvironment(FormDef form, Mockup mockup, SessionWrapper sessionWrapper) {
        this(form);
        this.mockup = mockup;
        this.sessionWrapper = sessionWrapper;
    }

    public FormEntryController setup() {
        return setup(createIIF());
    }
    
    public FormEntryController setup(InstanceInitializationFactory factory) {
        form.setEvaluationContext(getEC());

        form.initialize(true, factory);

        if(recording) {
            session = new Session();
            currentStep = new Step();
        } else {
            currentStep = session.getSteps().elementAt(0);
        }

        fem = new FormEntryModel(form);
        fec = new FormEntryController(fem);

        return fec;
    }

    public Step popStep() {
        if(!recording) {
            Step toRet = currentStep;
            stepCount++;
            if(session.getSteps().size() > stepCount) {
                currentStep = session.getSteps().elementAt(stepCount);
            } else {
                currentStep = null;
            }
            return toRet;
        } else {
            throw new IllegalStateException("Can't get step records in playback mode");
        }
    }

    private InstanceInitializationFactory createIIF() {
        return new CommCareInstanceInitializer(sessionWrapper.getSandbox(), sessionWrapper.getPlatform());
    }

    private EvaluationContext getEC() {
        EvaluationContext ec = new EvaluationContext(null);
        ec.addFunctionHandler(new TodayFunc("today"));
        ec.addFunctionHandler(new TodayFunc("now"));
        return ec;
    }

    public void setLocale(String locale) {
        for(String existingLocale : this.form.getLocalizer().getAvailableLocales()) {
            if(existingLocale.equals(locale)) {
                this.form.getLocalizer().setLocale(locale);
                return;
            }
        }
    }

    private class TodayFunc implements IFunctionHandler {

        String name;

        public TodayFunc(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Vector getPrototypes() {
            Vector p = new Vector();
            p.addElement(new Class[0]);
            return p;
        }

        public boolean rawArgs() {
            return false;
        }

        public boolean realTime() {
            return true;
        }

        public Object eval(Object[] args, EvaluationContext ec) {
            if(mockup != null && mockup.getDate() != null) {
                return mockup.getDate();
            } else {
                return new Date();
            }
        }

    }

    public void commitStep() {
        if(recording) {
            session.addStep(currentStep);
            currentStep = new Step();
        }
    }


    public void recordAction(Action action) {
        if(recording) {
            currentStep.setAction(action);
        }
    }

    public boolean isModePlayback() {
        return !recording;
    }

    public Session getSessionRecording() {
        if(recording) {
            return session;
        } else {
            throw new IllegalStateException("Can't get a recording from a playback session!");
        }
    }
}
