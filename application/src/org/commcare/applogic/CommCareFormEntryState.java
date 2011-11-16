package org.commcare.applogic;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.services.AutomatedSenderService;
import org.commcare.suite.model.Profile;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareSense;
import org.commcare.util.CommCareUtil;
import org.commcare.util.FormTransportWorkflow;
import org.javarosa.cases.instance.CaseInstanceTreeElement;
import org.javarosa.cases.model.Case;
import org.javarosa.cases.util.CaseModelProcessor;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.SubmissionProfile;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.formmanager.api.FormEntryState;
import org.javarosa.formmanager.api.JrFormEntryController;
import org.javarosa.formmanager.properties.FormManagerProperties;
import org.javarosa.formmanager.utility.FormDefFetcher;
import org.javarosa.formmanager.utility.NamespaceRetrievalMethod;
import org.javarosa.formmanager.view.IFormEntryView;
import org.javarosa.formmanager.view.chatterbox.Chatterbox;
import org.javarosa.formmanager.view.singlequestionscreen.SingleQuestionView;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.services.transport.SubmissionTransportHelper;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportException;
import org.javarosa.xpath.XPathParseTool;

//can't support editing saved forms; for new forms only
public abstract class CommCareFormEntryState extends FormEntryState {
	
	public static final String METHOD_SMS = "smspush";
	public static final String METHOD_POST = "post";
	private String formName;
	private Vector<IPreloadHandler> preloaders;
	private Vector<IFunctionHandler> funcHandlers;
	private InstanceInitializationFactory iif;
	String title;
		
	public CommCareFormEntryState (String title,String formName,
			Vector<IPreloadHandler> preloaders, Vector<IFunctionHandler> funcHandlers, InstanceInitializationFactory iif) {
		this.title = title;
		this.formName = formName;
		this.preloaders = preloaders;
		this.funcHandlers = funcHandlers;
		this.iif = iif;
	}
	
	protected JrFormEntryController getController() {
		FormDefFetcher fetcher = new FormDefFetcher(new NamespaceRetrievalMethod(formName), preloaders, funcHandlers,iif);
		JrFormEntryController controller = CommCareUtil.createFormEntryController(fetcher);
		controller.setView(loadView(title,controller));
		return controller;
	}
	
	private IFormEntryView loadView(String title, JrFormEntryController controller) {
		String viewType = PropertyManager._().getSingularProperty(FormManagerProperties.VIEW_TYPE_PROPERTY);		
		
		if (FormManagerProperties.VIEW_CHATTERBOX.equals(viewType)) {
			return new Chatterbox(title, controller);
			
		} else if (FormManagerProperties.VIEW_SINGLEQUESTIONSCREEN.equals(viewType)) {
			return new SingleQuestionView(controller, title);
			
		} else {
			return new Chatterbox(title, controller);
		}
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
			
			// This process will take place in three parts, in order to ensure that the server and client don't get
			// out of sync. 
			
			
			// 1) First we'll ensure that a transport message or other off-line element can be created and
			// saved. This should establish that it will be sent from the device no matter what.		
			
			//First, figure out what the submission action is
			SubmissionProfile profile = CommCareFormEntryState.getSubmissionProfile(form, instanceData);
			
			// Get the workflow that will be responsible for managing the flow of form sending
			FormTransportWorkflow workflow = getWorkflowFactory(profile);
			
			//Let the workflow perform its first action
			workflow.preProcessing(instanceData);
			
			
			// 2) Next we'll post process the data and manage any local information that should be handled on
			// the local device
			
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
			
			// 3) Actually manage what to do after the data is processed locally
			workflow.postProcessing();
		} else {
			abort();
		}
	}
	
	protected static SubmissionProfile getSubmissionProfile(FormDef form, FormInstance instanceData) {
		
		// See if the form wants to force a specific submission type
		SubmissionProfile profile = form.getSubmissionProfile();
		
		//TODO: Are there any circumstances in which the phone's send type should override the phone's options?
		if(profile == null) {
			String style = PropertyManager._().getSingularProperty(CommCareProperties.SEND_STYLE);
			if(CommCareProperties.SEND_STYLE_HTTP.equals(style)) {
				//CAN'T BE SERIALIZED
				profile = new SubmissionProfile(new XPathReference("/"),METHOD_POST,CommCareContext._().getSubmitURL(),null);
			} else {
				//No idea
				profile = new SubmissionProfile(new XPathReference("/"),PropertyManager._().getSingularProperty(CommCareProperties.SEND_STYLE),null,null);
			}

		}
		
		//See if the action is actually a reference into the form.
		String action = profile.getAction();
		try {
			String newAction = (String)XPathParseTool.parseXPath("string(" + action + ")").eval(instanceData, new EvaluationContext(form.exprEvalContext, instanceData.getRoot().getRef()));
			
			profile = new SubmissionProfile(profile.getRef(), profile.getMethod(), newAction, profile.getMediaType());
		} catch(Exception e) {
			//Expected use case for most static #'s.
			e.printStackTrace();
		}
		
		return profile;
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
	
	/**
	 *  Get the workflow for how to manage each step in the pre/post processing workflow.
	 *  
	 *  TODO: This can/should be shared with JavaRosa core, probably. Safest way to stay in sync
	 *  WRT SMS transport, etc.
	 */
	private FormTransportWorkflow getWorkflowFactory(final SubmissionProfile profile) {
		if(CommCareProperties.SEND_STYLE_NONE.equals(profile.getMethod())) {
			return new FormTransportWorkflow() {
				
				public void preProcessing(FormInstance instance) {
					// Nothing
				}
				public void postProcessing() {
					CommCareFormEntryState.this.goHome();
				}
			};
		} else if(CommCareProperties.SEND_STYLE_FILE.equals(profile.getMethod())) {
			return new FormTransportWorkflow() {
				public void preProcessing(FormInstance instance) {
					// TODO: Save data to file system
				}
				public void postProcessing() {
					CommCareFormEntryState.this.goHome();
				}
			};
				
		} else {
			return new FormTransportWorkflow() {
				TransportMessage message;
				boolean cacheable;
				
				public void preProcessing(FormInstance instance) {
					//HTTP or Fallback(Nothing)
					//This actually won't send anything just yet. It'll just put it straight in the cache
					try {
						cacheable = profile.getMethod().equals(METHOD_POST);
						message = SubmissionTransportHelper.createMessage(instance, profile, cacheable);
						
						if(cacheable) {
							//cache it
							TransportService.send(message, 0, 0);
							Logger.log("formentry", "create txmsg " + PropertyUtils.trim(message.getCacheIdentifier(), 6));
						} else{
							Logger.log("formentry", "created message of type " + profile.getMethod() + ", and autosending");
							//send it
							TransportService.send(message, 3, 0);
						}
						
						// If there is a failure building or caching the transport message, we don't want to further process data.
						// Otherwise the phone will have data that can never get to the server.
					} catch (IOException e) { 
						e.printStackTrace();
						Logger.die("create-form-message", e);
						return;
					} catch (TransportException e) {
						e.printStackTrace();
						Logger.die("create-form-message", e);
						return;
					} 
				}
				public void postProcessing() {
					CommCarePostFormEntryState httpAskSendState = new CommCarePostFormEntryState(message, CommCareSense.isAutoSendEnabled() || !cacheable) {
						public void goHome() {
							//If we're autosending, make sure to expire old deadlines
							if(CommCareSense.isAutoSendEnabled()) {
								//Notify the service that old deadlines have expired.
								AutomatedSenderService.NotifyPending();
							}
							CommCareFormEntryState.this.goHome();
						}
					};
					
					
					J2MEDisplay.startStateWithLoadingScreen(httpAskSendState);
				}
			};
		}

	}
}
