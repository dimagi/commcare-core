package org.javarosa.engine;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.engine.models.Action;
import org.javarosa.engine.models.Mockup;
import org.javarosa.engine.models.Session;
import org.javarosa.engine.models.Step;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;

import java.util.Date;
import java.util.Hashtable;

/**
 *
 * Run an XForm programatically for fun and profit.
 *
 * @author ctsims
 */
public class XFormEnvironment {

    private final FormDef form;

    private String preferredLocale;

    private Step currentStep;
    private int stepCount = 0;

    private Session session;
    private Mockup mockup;
    private Date hardCodedDate;
    boolean recording = true;

    public XFormEnvironment(FormDef form) {
        this.form = form;
    }

    public XFormEnvironment(FormDef form, Session session) {
        this.form = form;
        this.session = session;
        recording = false;
    }

    public XFormEnvironment(FormDef form, Mockup mockup) {
        this(form);
        this.mockup = mockup;
    }

    public FormEntryController setup() {
        return setup(createIIF());
    }
    
    public FormEntryController setup(InstanceInitializationFactory factory) {
        form.setEvaluationContext(getEC());

        form.initialize(true, factory, preferredLocale);

        if(recording) {
            session = new Session();
            currentStep = new Step();
        } else {
            currentStep = session.getSteps().elementAt(0);
        }

        FormEntryModel fem = new FormEntryModel(form);
        FormEntryController fec = FormEntryController.buildRecordingController(fem);

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
        return new MockupProviderFactory(mockup == null ? new Hashtable() : mockup.getInstances());
    }

    private EvaluationContext getEC() {
        EvaluationContext ec = new EvaluationContext(null);
        Date customDate = hardCodedDate;
        if (mockup != null) {
            customDate = mockup.getDate();
        }
        ec.addFunctionHandler(new FunctionExtensions.TodayFunc("today", customDate));
        ec.addFunctionHandler(new FunctionExtensions.TodayFunc("now", customDate));
        ec.addFunctionHandler(new FunctionExtensions.PrintFunc(createIIF()));
        return ec;
    }

    public void setLocale(String locale) {
        for(String existingLocale : this.form.getLocalizer().getAvailableLocales()) {
            if(existingLocale.equals(locale)) {
                preferredLocale = locale;
                return;
            }
        }
    }

    public void setToday(String dateString) {
        hardCodedDate = DateUtils.parseDate(dateString);
        System.out.println(hardCodedDate);
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
