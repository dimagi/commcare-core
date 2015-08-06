package org.commcare.applogic;

import org.commcare.cases.util.CaseModelProcessor;
import org.commcare.core.properties.CommCareProperties;
import org.commcare.suite.model.Profile;
import org.commcare.util.CommCareContext;
import org.commcare.util.CommCareSense;
import org.commcare.util.CommCareUtil;
import org.commcare.util.FormTransportWorkflow;
import org.javarosa.core.data.IDataPointer;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.SubmissionProfile;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.data.PointerAnswerData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.UnavailableServiceException;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.formmanager.api.FormEntryState;
import org.javarosa.formmanager.api.JrFormEntryController;
import org.javarosa.formmanager.api.transitions.FormEntryTransitions;
import org.javarosa.formmanager.properties.FormManagerProperties;
import org.javarosa.formmanager.utility.FormDefFetcher;
import org.javarosa.formmanager.utility.NamespaceRetrievalMethod;
import org.javarosa.formmanager.view.IFormEntryView;
import org.javarosa.formmanager.view.chatterbox.Chatterbox;
import org.javarosa.formmanager.view.singlequestionscreen.SingleQuestionView;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.util.CommCareHandledExceptionState;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.media.image.activity.ImageCaptureState;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.services.transport.SubmissionTransportHelper;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportException;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;
import org.javarosa.xpath.XPathMissingInstanceException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.XPathTypeMismatchException;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

//can't support editing saved forms; for new forms only
public abstract class CommCareFormEntryState extends FormEntryState {

    public static final String METHOD_SMS = "smspush";
    public static final String METHOD_POST = "post";
    private String formName;
    private Vector<IPreloadHandler> preloaders;
    private Vector<IFunctionHandler> funcHandlers;
    private InstanceInitializationFactory iif;
    String title;
    //Keep this alive for the lifecycle of the state.
    private CommCareHandledExceptionState cches;

    public CommCareFormEntryState (String title,String formName,
            Vector<IPreloadHandler> preloaders, Vector<IFunctionHandler> funcHandlers, InstanceInitializationFactory iif) {

        this.title = title;
        this.formName = formName;
        this.preloaders = preloaders;
        this.funcHandlers = funcHandlers;
        this.iif = iif;

        cches = new CommCareHandledExceptionState() {

            public boolean handlesException(Exception e) {
                return ((e instanceof XPathMissingInstanceException) || (e instanceof XPathTypeMismatchException));
            }

            public String getExplanationMessage(String e){
                return Localization.get("xpath.fail.runtime", new String[] {e});
            }

            public void done() {
                CommCareFormEntryState.this.abort();
            }
        };

        CrashHandler.setExceptionHandler(cches);
    }

    protected JrFormEntryController getController() {
        FormDefFetcher fetcher = new FormDefFetcher(new NamespaceRetrievalMethod(formName), preloaders, funcHandlers,iif);

        boolean supportsNewRepeats = false;
        String viewType = PropertyManager._().getSingularProperty(FormManagerProperties.VIEW_TYPE_PROPERTY);

        if (FormManagerProperties.VIEW_CHATTERBOX.equals(viewType)) {
            supportsNewRepeats = true;
        }

        JrFormEntryController controller = CommCareUtil.createFormEntryController(fetcher, supportsNewRepeats);
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

            form.seal();

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

    public void suspendForMediaCapture (int captureType) throws UnavailableServiceException {
        if(captureType ==FormEntryTransitions.MEDIA_IMAGE){
            ImageCaptureState ics = new ImageCaptureState() {

                public void cancel() {
                    controller.start();
                }

                public void captured(IDataPointer data) {
                    controller.answerQuestion(new PointerAnswerData(data));
                }

                public void captured(IDataPointer[] data) {

                }

                public void noCapture() {
                    controller.start();
                }

            };
            J2MEDisplay.startStateWithLoadingScreen(ics);
        } else {
            super.suspendForMediaCapture(captureType);
        }
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
                        //Maaaaan this is ugly
                        if(message instanceof SimpleHttpTransportMessage) {
                            if(CommCareContext._().getUser() != null && CommCareContext._().getUser().getLastSyncToken() != null) {
                                ((SimpleHttpTransportMessage)message).setHeader("X-CommCareHQ-LastSyncToken", CommCareContext._().getUser().getLastSyncToken());
                            }
                        }

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
                    CommCarePostFormEntryState httpAskSendState = new CommCarePostFormEntryState(message.getCacheIdentifier(), CommCareSense.isAutoSendEnabled() || !cacheable) {

                        /* (non-Javadoc)
                         * @see org.commcare.applogic.CommCarePostFormEntryState#goHome()
                         */
                        public void goHome() {
                            CommCareFormEntryState.this.goHome();
                        }

                    };

                    J2MEDisplay.startStateWithLoadingScreen(httpAskSendState);
                }
            };
        }

    }
}
