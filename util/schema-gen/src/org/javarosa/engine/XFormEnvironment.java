/**
 * 
 */
package org.javarosa.engine;

import java.util.Date;
import java.util.Vector;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.engine.models.Action;
import org.javarosa.engine.models.Session;
import org.javarosa.engine.models.Step;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;

/**
 *  
 * Run an XForm programatically for fun and profit.
 * 
 * @author ctsims
 *
 */
public class XFormEnvironment {
	
	private Date today = new Date();
	
	private FormDef form;
	
	private FormEntryModel fem;
	private FormEntryController fec;
	
	
	private Step currentStep;
	private int stepCount = 0;
	
	private Session session;
	boolean recording = true;

	public XFormEnvironment(FormDef form) {
		this.form = form;
	}
	
	public XFormEnvironment(FormDef form, Session session) {
		this.form = form;
		this.session = session;
		recording = false;
	}
	
	
	public void setToday(Date date) {
		today = date;
	}
	
	public FormEntryController setup() {
		form.setEvaluationContext(getEC());
		form.initialize(true, createIIF());
		
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
		return null;
	}
	
	private EvaluationContext getEC() {
		EvaluationContext ec = new EvaluationContext(null);
		ec.addFunctionHandler(new TodayFunc("today"));
		ec.addFunctionHandler(new TodayFunc("now"));
		return ec;
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
			return today;
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
