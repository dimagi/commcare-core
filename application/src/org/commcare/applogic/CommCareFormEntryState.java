package org.commcare.applogic;

import java.util.Date;
import java.util.Vector;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.suite.model.Profile;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareUtil;
import org.javarosa.cases.util.CaseModelProcessor;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.formmanager.api.FormEntryState;
import org.javarosa.formmanager.api.JrFormEntryController;
import org.javarosa.formmanager.utility.FormDefFetcher;
import org.javarosa.formmanager.utility.NamespaceRetrievalMethod;
import org.javarosa.formmanager.view.chatterbox.Chatterbox;
import org.javarosa.j2me.view.J2MEDisplay;

//can't support editing saved forms; for new forms only
public abstract class CommCareFormEntryState extends FormEntryState {

	private String formName;
	private Vector<IPreloadHandler> preloaders;
	private Vector<IFunctionHandler> funcHandlers;
	String title;
		
	public CommCareFormEntryState (String title,String formName,
			Vector<IPreloadHandler> preloaders, Vector<IFunctionHandler> funcHandlers) {
		this.title = title;
		this.formName = formName;
		this.preloaders = preloaders;
		this.funcHandlers = funcHandlers;
	}
	
	protected JrFormEntryController getController() {
		FormDefFetcher fetcher = new FormDefFetcher(new NamespaceRetrievalMethod(formName), preloaders, funcHandlers);
		JrFormEntryController controller = CommCareUtil.createFormEntryController(fetcher);
		controller.setView(new Chatterbox(title,controller));
		return controller;
	}
	
	public void abort () {
		goHome();
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.formmanager.api.transitions.FormEntryTransitions#formEntrySaved(org.javarosa.core.model.FormDef, org.javarosa.core.model.instance.FormInstance, boolean)
	 */
	public void formEntrySaved(FormDef form, FormInstance instanceData, boolean formWasCompleted) {
		if(formWasCompleted) {
			logCompleted(instanceData);
			boolean save = postProcess(instanceData);
			if (save) {
				IStorageUtility instances = StorageManager.getStorage(FormInstance.STORAGE_KEY);
		        try {
		        	instanceData.setDateSaved(new Date());
		        	instances.write(instanceData);
		        } catch (StorageFullException e) {
					throw new RuntimeException("uh-oh, storage full [saved forms]"); //TODO: handle this
		        }
		        postSaveProcess(instanceData);
			}
			
			
			//Figure out what to do...
			String action = PropertyManager._().getSingularProperty(CommCareProperties.SEND_STYLE);
			
			//This name is generic, but it's actually HTTP only
			if(action == null || CommCareProperties.SEND_STYLE_HTTP.equals(action)) {
				send(instanceData);
				return;
			} else if(CommCareProperties.SEND_STYLE_NONE.equals(action)) {
				CommCareFormEntryState.this.goHome();
			}else if(CommCareProperties.SEND_STYLE_FILE.equals(action)) {
				//TODO: File Save here...
				CommCareFormEntryState.this.goHome();
			} else {
				//The 'Ol Fallback.
				send(instanceData);
			}
		} else {
			abort();
		}
	}
	
	private void send(FormInstance instanceData) {
		J2MEDisplay.startStateWithLoadingScreen(new CommCarePostFormEntryState(instanceData) {
			public void goHome() {
				CommCareFormEntryState.this.goHome();
			}
		});
	}

	public void suspendForMediaCapture(int captureType) {
		throw new RuntimeException("transition not applicable");
	}

	/**
	 * 
	 * @param instanceData
	 * @return true if instance should be saved in persistent storage
	 */
	protected boolean postProcess (FormInstance instanceData) {
		CaseModelProcessor processor = new CaseModelProcessor();
		processor.processInstance(instanceData);
		//If we're doing reviews, we need to save instances!
		if(CommCareContext._().getManager().getCurrentProfile().isFeatureActive(Profile.FEATURE_REVIEW)) {			
			return true;
		} else{
			return false;
		}
	}
	
	/**
	 * post-processing to be done after (and only if) form is saved
	 * @param instanceData
	 * @return
	 */
	protected void postSaveProcess (FormInstance instanceData) {
		//do nothing
	}
	
	protected abstract void goHome();
	
	protected void logCompleted (FormInstance instanceData) {
		String guid;
		try {
			guid = (String)RestoreUtils.getValue("Meta/uid", instanceData);
		} catch (RuntimeException e) {
			guid = "?";
		}
		
		Logger.log("form-completed", instanceData.getFormId() + ":" + PropertyUtils.trim(guid, 12));
	}
}
