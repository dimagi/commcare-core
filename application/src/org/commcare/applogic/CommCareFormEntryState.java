package org.commcare.applogic;

import java.util.Date;
import java.util.Vector;

import org.commcare.suite.model.Profile;
import org.commcare.util.CommCareContext;
import org.javarosa.cases.util.CaseModelProcessor;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.formmanager.api.FormEntryState;
import org.javarosa.formmanager.api.JrFormEntryController;
import org.javarosa.formmanager.api.JrFormEntryModel;
import org.javarosa.formmanager.utility.FormDefFetcher;
import org.javarosa.formmanager.utility.NamespaceRetrievalMethod;
import org.javarosa.formmanager.view.chatterbox.Chatterbox;

//can't support editing saved forms; for new forms only
public abstract class CommCareFormEntryState extends FormEntryState {

	private String formName;
	private Vector<IPreloadHandler> preloaders;
	private Vector<IFunctionHandler> funcHandlers;
		
	public CommCareFormEntryState (String formName,
			Vector<IPreloadHandler> preloaders, Vector<IFunctionHandler> funcHandlers) {
		this.formName = formName;
		this.preloaders = preloaders;
		this.funcHandlers = funcHandlers;
	}
	
	protected JrFormEntryController getController() {
		FormDefFetcher fetcher = new FormDefFetcher(new NamespaceRetrievalMethod(formName), preloaders, funcHandlers);
		JrFormEntryController controller = new JrFormEntryController(new JrFormEntryModel(fetcher.getFormDef()));
		controller.setView(new Chatterbox("Chatterbox",controller));
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
			
			new CommCarePostFormEntryState(instanceData) {
				public void goHome() {
					CommCareFormEntryState.this.goHome();
				}
			}.start();
		} else {
			abort();
		}
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
		
		Logger.log("form-completed", instanceData.getFormId() + ":" + guid);
	}
}
