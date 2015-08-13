/**
 *
 */
package org.commcare.util;

import org.commcare.applogic.CommCareUpgradeState;
import org.commcare.cases.CaseManagementModule;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.ledger.LedgerPurgeFilter;
import org.commcare.cases.model.Case;
import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.core.properties.CommCareProperties;
import org.commcare.model.PeriodicEvent;
import org.commcare.model.PeriodicEventRecord;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.TableStateListener;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.services.AutomatedSenderService;
import org.commcare.util.time.AutoSyncEvent;
import org.commcare.util.time.AutoUpdateEvent;
import org.commcare.util.time.PermissionsEvent;
import org.commcare.util.time.TimeMessageEvent;
import org.commcare.view.CommCareStartupInteraction;
import org.commcare.xml.CommCareElementParser;
import org.javarosa.core.model.CoreModelModule;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.services.storage.WrappingStorageUtility.SerializationWrapper;
import org.javarosa.core.util.JavaRosaCoreModule;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.formmanager.FormManagerModule;
import org.javarosa.formmanager.properties.FormManagerProperties;
import org.javarosa.j2me.J2MEModule;
import org.javarosa.j2me.file.J2meFileReference;
import org.javarosa.j2me.file.J2meFileRoot;
import org.javarosa.j2me.file.J2meFileSystemProperties;
import org.javarosa.j2me.reference.HttpReference.SecurityFailureListener;
import org.javarosa.j2me.storage.rms.RMSRecordLoc;
import org.javarosa.j2me.storage.rms.RMSStorageUtility;
import org.javarosa.j2me.storage.rms.RMSStorageUtilityIndexed;
import org.javarosa.j2me.storage.rms.RMSTransaction;
import org.javarosa.j2me.util.DumpRMS;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.log.LogManagementModule;
import org.javarosa.log.util.LogReportUtils;
import org.javarosa.model.xform.XFormsModule;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.resources.locale.LanguagePackModule;
import org.javarosa.resources.locale.LanguageUtils;
import org.javarosa.service.transport.securehttp.AuthUtils;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;
import org.javarosa.service.transport.securehttp.HttpCredentialProvider;
import org.javarosa.services.transport.TransportManagerModule;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportMessageSerializationWrapper;
import org.javarosa.services.transport.impl.TransportMessageStore;
import org.javarosa.user.activity.UserModule;
import org.javarosa.user.model.User;
import org.javarosa.user.utility.UserPreloadHandler;
import org.javarosa.user.utility.UserUtility;
import org.javarosa.xform.util.XFormUtils;
import org.javarosa.xml.util.UnfullfilledRequirementsException;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.file.FileConnection;
import javax.microedition.midlet.MIDlet;

import de.enough.polish.ui.Display;

/**
 * @author ctsims
 *
 */
public class CommCareContext {

    private static CommCareContext i;

    private MIDlet midlet;
    private String loggedInUserID;

    private CommCarePlatform manager;

    protected boolean inDemoMode;

    /** We'll store the credential provider internally to be produced first in syncing **/
    private HttpCredentialProvider userCredentials;

    public String getSubmitURL() {
        String url = PropertyManager._().getSingularProperty(CommCareProperties.POST_URL_PROPERTY);

        String testUrl = PropertyManager._().getSingularProperty(CommCareProperties.POST_URL_TEST_PROPERTY);
        if(CommCareUtil.isTestingMode() && testUrl != null) {
            //In testing mode, use this URL instead, if available.
            url = testUrl;
        }
        return url;
    }

    public MIDlet getMidlet() {
        return midlet;
    }

    public CommCarePlatform getManager() {
        return manager;
    }

    public void configureApp(MIDlet m, InitializationListener listener) {
        //Application Entry point should be considered to be here

        failsafeInit(m);
        Logger.log("app-start", "");

        this.midlet = m;

        setProperties();
        loadModules();

        registerAddtlStorage();
        StorageManager.repairAll();
        RMSTransaction.cleanup();

        initReferences();

        Localization.registerLanguageReference("default","jr://resource/messages_cc_default.txt");
        Localization.registerLanguageReference("sw","jr://resource/messages_cc_sw.txt");


        final CommCareStartupInteraction interaction = new CommCareStartupInteraction(CommCareStartupInteraction.failSafeText("commcare.init", "CommCare is Starting..."));
        Display.getDisplay(m).setCurrent(interaction);

        CommCareInitializer initializer = new CommCareInitializer() {

            int currentProgress = 0;
            int block = 0;

            private String validate() {
                interaction.setMessage(CommCareStartupInteraction.failSafeText("install.verify","CommCare initialized. Validating multimedia files..."));
                return CommCareStatic.validate(CommCareContext.RetrieveGlobalResourceTable());
            }

            protected boolean runWrapper() throws UnfullfilledRequirementsException {
                updateProgress(10);

                //TODO: Make this update progress, takes forever
                //Real quick, go trigger an index build to sure we do this when we have the most memory possible
                ((IStorageUtilityIndexed)StorageManager.getStorage(FormDef.STORAGE_KEY)).getIDsForValue("XMLNS", "");

                //Clear out any resources from any botched installations
                CommCareContext.ClearUpdateTable();


                manager = new CommCarePlatform(CommCareUtil.getMajorVersion(), CommCareUtil.getMinorVersion());

                //Try to initialize and install the application resources...
                try {

                    //TODO: This cleanup is replicated across different parts of code.
                    ResourceTable global = RetrieveGlobalResourceTable();

                    ResourceTable upgrade = CommCareContext.CreateTemporaryResourceTable(CommCareUpgradeState.UPGRADE_TABLE_NAME);

                    /**
                     * See if any of our tables got left in a weird state
                     */

                    if(global.getTableReadiness() == ResourceTable.RESOURCE_TABLE_UNCOMMITED) {
                        global.rollbackCommits();
                    }


                    if(upgrade.getTableReadiness() == ResourceTable.RESOURCE_TABLE_UNCOMMITED) {
                        upgrade.rollbackCommits();
                    }

                    /**
                     * See if we got left in the middle of an update, make sure we get fixed.
                     *
                     */

                    if(global.getTableReadiness() == ResourceTable.RESOURCE_TABLE_UNSTAGED) {
                        //If so, repair the global table. (Always takes priority over maintaining
                        //the update)
                        global.repairTable(upgrade);
                    }



                    global.setStateListener(new TableStateListener() {

                        static final int INSTALL_SCORE = 5;
                        public void resourceStateUpdated(ResourceTable table) {
                            setCurrentOOMMessage(CommCareStartupInteraction.failSafeText("commcare.install.oom","CommCare needs to restart in order to continue installing your application. Please press 'OK' and start CommCare again."));
                            int score = 0;
                            int max = 0;
                            Vector<Resource> resources = CommCareResourceManager.getResourceListFromProfile(table);
                            max = resources.size() * INSTALL_SCORE;

                            if(max <= INSTALL_SCORE*2) {
                                //Apps have to have at least 1 resource (profile), and won't really work without a suite, and
                                //we don't want to jump around too much past that, so we won't bother updating the slider until
                                //we've found at least those.
                                return;
                            }

                            for(Resource r : resources) {
                                switch(r.getStatus()) {
                                case Resource.RESOURCE_STATUS_INSTALLED:
                                    score += INSTALL_SCORE;
                                    break;
                                default:
                                    score += 1;
                                    break;
                                }
                            }
                            updateProgress(10 + (int)Math.ceil(50 * (score * 1.0 / max)));
                        }

                        public void incrementProgress(int complete, int total) {
                            // TODO Auto-generated method stub
                            updateProgress(currentProgress + (int)Math.ceil(block * (complete * 1.0 / total)));
                        }
                    });

                    String profileRef = CommCareUtil.getProfileReference();
                    if(global.isEmpty()) {
                        this.setMessage(CommCareStartupInteraction.failSafeText("commcare.firstload","First start detected, loading resources..."));
                        if(profileRef == null) {
                            String message = "CommCare could not find any application configuration data to install. Please make sure that all CommCare install files are present.";

                            //#ifdef polish.group.Series40
                            //# message = "CommCare cannot find the CommCare.jad file. Please ensure that it is placed in the same install folder as CommCare.jar";
                            //#endif

                            throw new RuntimeException(message);
                        }
                    }
                    manager.init(profileRef, global, false);
                    updateProgress(60);

                } catch (UnfullfilledRequirementsException e) {
                    if(e.getSeverity() == CommCareElementParser.SEVERITY_PROMPT) {
                        String message = e.getMessage();
                        if(e.getRequirementCode() == CommCareElementParser.REQUIREMENT_MAJOR_APP_VERSION || e.getRequirementCode() == CommCareElementParser.REQUIREMENT_MAJOR_APP_VERSION) {
                            message = CommCareStartupInteraction.failSafeText("commcare.badversion",
                                    "The application requires a newer version of CommCare than is installed. It may not work correctly. Should installation be attempted anyway?");
                        }
                        if(this.blockForResponse(message)) {
                            try {
                                //If we're going to try to run commcare with an incompatible version, first clear everything
                                RetrieveGlobalResourceTable().clear();
                                manager.init(CommCareUtil.getProfileReference(), RetrieveGlobalResourceTable(), true);
                            } catch (UnfullfilledRequirementsException e1) {
                                //Maybe we should try to clear the table here, too?
                                throw e1;
                            }  catch (UnresolvedResourceException e3) {
                                //this whole process needs to be cleaned up
                                throw new RuntimeException(e3.getMessage());
                            }
                        } else {
                            throw e;
                        }
                    } else {
                        throw e;
                    }
                } catch (UnresolvedResourceException e) {
                    //this whole process needs to be cleaned up
                    throw new RuntimeException("Error installing resource: " + e.getResource().getDescriptor() + "\n" + e.getMessage());
                }

                currentProgress = 60;
                block = 30;
                if(!CommCareUtil.getAppProperty("Skip-Validation","no").equals("yes") && !CommCareProperties.PROPERTY_YES.equals(PropertyManager._().getSingularProperty(CommCareProperties.CONTENT_VALIDATED))) {
                    String failureMessage = this.validate();
                    while(failureMessage != null) {
                        Logger.log("startup", "Missing Resources on startup");
                        this.blockForResponse(failureMessage, "Retry", "No");
                        if(this.response == CommCareInitializer.RESPONSE_YES) {
                            failureMessage = this.validate();
                        } else {
                            //TODO: We need to set a flag which says that CommCare can't start up.
                            CommCareContext.this.exitApp();
                            return false;
                        }
                    }
                    PropertyManager._().setProperty(CommCareProperties.CONTENT_VALIDATED, CommCareProperties.PROPERTY_YES);
                }

                updateProgress(90);

                //When we might initialize language files, we need to make sure it's not trying
                //to load any of them into memory, since the default ones are not guaranteed to
                //be added later.
                Localization.setLocale("default");
                manager.initialize(RetrieveGlobalResourceTable());

                purgeScheduler(false);

                //Now that the profile has had a chance to set properties (without them requiring
                //override) set the fallback defaults.
                postProfilePropertyInit();

                initUserFramework();

                //Establish default logging deadlines
                LogReportUtils.initPendingDates(new Date().getTime());

                //Now we can initialize the language for real.
                LanguageUtils.initializeLanguage(true,"default");

                updateProgress(95);

                //We need to let All Localizations register before we can do this
                J2MEDisplay.init(CommCareContext.this.midlet);

                if(CommCareSense.isAutoSendEnabled()) {
                    AutomatedSenderService.InitializeAndSpawnSenderService();
                }

                return true;
            }

            protected void askForResponse(String message, YesNoListener yesNoListener, boolean yesNo) {
                if(yesNo) {
                    interaction.AskYesNo(message,yesNoListener);
                } else {
                    interaction.PromptResponse(message, yesNoListener);
                }
            }

            protected void askForResponse(String message, YesNoListener yesNoListener, boolean yesNo, String left, String right) {
                if(yesNo) {
                    interaction.AskYesNo(message,yesNoListener, left, right);
                } else {
                    interaction.PromptResponse(message, yesNoListener);
                }
            }


            protected void setMessage(String message) {
                interaction.setMessage(message, true);
            }

            protected void updateProgress(int progress) {
                interaction.updateProgess(progress);
            }
        };

        initializer.initialize(listener);
    }

    private void failsafeInit (MIDlet m) {
        DumpRMS.RMSRecoveryHook(m);

        String fileSystemTranslator = m.getAppProperty("FileRootTranslator");
        boolean useRealFiles = true;
        if(fileSystemTranslator != null) {
            useRealFiles = false;
            ReferenceManager._().addRootTranslator(new RootTranslator("jr://file/",fileSystemTranslator));
        }

        //TODO: Hilarious? Yes. Reasonable? No.

        //#if !j2merosa.disable.autofile
        new J2MEModule(new J2meFileSystemProperties(useRealFiles) {
            protected J2meFileRoot root(String root) {
                return new J2meFileRoot(root) {
                    protected Reference factory(String terminal, String URI) {
                        return new J2meFileReference(localRoot,  terminal) {
                            protected FileConnection connector(String URI, boolean cache) throws IOException {
                                try {
                                    return super.connector(URI, cache);
                                } catch(SecurityException se) {
                                    PeriodicEvent.schedule(new PermissionsEvent());
                                    //Should get swallowed
                                    throw new IOException("Couldn't access data at " + this.getLocalURI() + " due to lack of permissions.");
                                }
                            }
                        };
                    }
                };
            }
            protected void securityException(SecurityException e) {
                super.securityException(e);
                PeriodicEvent.schedule(new PermissionsEvent());
            }
        }) {
            protected void postStorageRegistration() {
                //immediately after the storage is registered, we want to catch file system events
                StorageManager.registerStorage(PeriodicEventRecord.STORAGE_KEY, PeriodicEventRecord.class);
                super.postStorageRegistration();
            }
        }.registerModule();
        //#else
        //new J2MEModule().registerModule();
        ////Since this is being registered in the fancy module init above
        //StorageManager.registerStorage(PeriodicEventRecord.STORAGE_KEY, PeriodicEventRecord.class);
        //#endif
    }

    private void initUserFramework() {
        UserUtility.populateAdminUser(midlet);
        inDemoMode = false;
        String namespace = PropertyUtils.initializeProperty(CommCareProperties.USER_REG_NAMESPACE, "http://code.javarosa.org/user_registration");

        if(namespace.equals("http://code.javarosa.org/user_registration")) {
            IStorageUtilityIndexed formDefStorage = (IStorageUtilityIndexed)StorageManager.getStorage(FormDef.STORAGE_KEY);
            Vector forms = formDefStorage.getIDsForValue("XMLNS", namespace);
            if(forms.size() == 0) {
                //Default user registration form isn't present, parse if from the default location.
                try {
                    formDefStorage.write(XFormUtils.getFormFromInputStream(ReferenceManager._().DeriveReference("jr://resource/register_user.xhtml").getStream()));
                } catch (IOException e) {
                    //I dunno? Log it?
                    e.printStackTrace();
                } catch (InvalidReferenceException e) {
                    // TODO Auto-referralCache catch block
                    e.printStackTrace();
                } catch (StorageFullException e) {
                    // TODO Auto-referralCache catch block
                    e.printStackTrace();
                }
            }
        }
    }

    protected void registerAddtlStorage () {
        //do nothing
        StorageManager.registerStorage("fixture", FormInstance.class);
    }

    protected void initReferences() {
        ReferenceManager._().addRootTranslator(new RootTranslator("jr://media/","jr://resource/img/"));
    }

    private void loadModules() {
        //So that properties appear at the bottom...
        new LogManagementModule().registerModule();
        new JavaRosaCoreModule().registerModule();
        new UserModule().registerModule();
        new LanguagePackModule().registerModule();
        new CoreModelModule().registerModule();
        new XFormsModule().registerModule();
        new CaseManagementModule().registerModule();
        new TransportManagerModule(new SecurityFailureListener(){
            public void onSecurityException(SecurityException e) {
                PeriodicEvent.schedule(new PermissionsEvent());
            }
        }).registerModule();
        new CommCareModule().registerModule();
        new FormManagerModule().registerModule();
    }

    protected void setProperties() {

        //NOTE: These properties should all be properties which are not expected to
        //be set by the profile, otherwise the profile will need to override the existing property.
        //Put generic fallbacks into postProfile property intiializer below.
        PropertyManager._().addRules(new JavaRosaPropertyRules());
        PropertyManager._().addRules(new CommCareProperties());
        PropertyUtils.initalizeDeviceID();

        PropertyUtils.initializeProperty(CommCareProperties.IS_FIRST_RUN, CommCareProperties.PROPERTY_YES);
        PropertyManager._().setProperty(CommCareProperties.COMMCARE_VERSION, CommCareUtil.getVersion());
        PropertyUtils.initializeProperty(CommCareProperties.DEPLOYMENT_MODE, CommCareProperties.DEPLOY_DEFAULT);

        //NOTE: Don't put any properties here which should be able to be override inside of the app profile. Users
        //should be able to override most properties without forcing.
    }


    private void postProfilePropertyInit() {
        PropertyUtils.initializeProperty(FormManagerProperties.EXTRA_KEY_FORMAT, FormManagerProperties.EXTRA_KEY_LANGUAGE_CYCLE);
        PropertyUtils.initializeProperty(CommCareProperties.ENTRY_MODE, CommCareProperties.ENTRY_MODE_QUICK);

        PropertyUtils.initializeProperty(CommCareProperties.SEND_STYLE, CommCareProperties.SEND_STYLE_HTTP);
        PropertyUtils.initializeProperty(CommCareProperties.OTA_RESTORE_OFFLINE, "jr://file/commcare_ota_backup_offline.xml");
        PropertyUtils.initializeProperty(CommCareProperties.RESTORE_TOLERANCE, CommCareProperties.REST_TOL_LOOSE);
        PropertyUtils.initializeProperty(CommCareProperties.DEMO_MODE, CommCareProperties.DEMO_ENABLED);
        PropertyUtils.initializeProperty(CommCareProperties.TETHER_MODE, CommCareProperties.TETHER_PUSH_ONLY);
        PropertyUtils.initializeProperty(CommCareProperties.LOGIN_IMAGE, "jr://resource/icon.png");

        PropertyManager._().setProperty(CommCareProperties.COMMCARE_VERSION, CommCareUtil.getVersion());

        PropertyUtils.initializeProperty(CommCareProperties.USER_REG_TYPE, CommCareProperties.USER_REG_REQUIRED);

        PropertyUtils.initializeProperty(CommCareProperties.AUTO_UPDATE_FREQUENCY, CommCareProperties.FREQUENCY_NEVER);
    }

    public static void init(MIDlet m, InitializationListener listener) {
        i = new CommCareContext();
        try{
            i.configureApp(m, listener);
        } catch(Exception e) {
            Logger.die("Init!", e);
        }
    }

    public static CommCareContext _() {
        if(i == null) {
            throw new RuntimeException("CommCareContext must be initialized with the Midlet to be used.");
        }
        return i;
    }

    public void setUser (User u, HttpCredentialProvider userCredentials) {
        this.loggedInUserID = u.getUniqueId();
        this.userCredentials = userCredentials;
        AuthUtils.setStaticAuthenticator(new HttpAuthenticator(CommCareUtil.wrapCredentialProvider(userCredentials)));
    }

    public User getUser () {
        if(User.DEMO_USER.equals(loggedInUserID)) {
            return User.FactoryDemoUser();
        } else if(loggedInUserID != null) {
            return (User)((IStorageUtilityIndexed)StorageManager.getStorage(User.STORAGE_KEY)).getRecordForValue(User.META_UID, loggedInUserID);
        } else {
            return null;
        }
    }

    public HttpCredentialProvider getCurrentUserCredentials() {
        if( userCredentials != null) {
            return userCredentials;
        } else {
            return new UserCredentialProvider(CommCareContext._().getUser());
        }
    }

    public PeriodicEvent[] getEventDescriptors() {
        return new PeriodicEvent[] {new TimeMessageEvent(), new PermissionsEvent(), new AutoUpdateEvent(), new AutoSyncEvent()};
    }


    public Vector<IFunctionHandler> getFuncHandlers () {
        Vector<IFunctionHandler> handlers = new Vector<IFunctionHandler>();
        handlers.addElement(new HouseholdExistsFuncHandler());
        handlers.addElement(new CHWReferralNumFunc()); //BHOMA custom!
        return handlers;
    }

    /// Probably put this stuff into app specific ones.
    public Vector<IPreloadHandler> getPreloaders() {
        Vector<IPreloadHandler> handlers = new Vector<IPreloadHandler>();

        MetaPreloadHandler meta = new MetaPreloadHandler(this.getUser());
        handlers.addElement(meta);
        handlers.addElement(new UserPreloadHandler(this.getUser()));
        return handlers;
    }

    private void registerDemoStorage (String key, Class type) {
        StorageManager.registerStorage(key, "DEMO_" + key, type);
    }

    private void registerWrappedDemoStorage(String key, SerializationWrapper wrapper) {
        StorageManager.registerWrappedStorage(key, "DEMO_" + key, wrapper);
    }

    public void toggleDemoMode(boolean demoOn) {
        if (demoOn != inDemoMode) {
            CommCareUtil.cycleDemoStyles(demoOn);
            inDemoMode = demoOn;
            if (demoOn) {
                registerDemoStorage(Case.STORAGE_KEY, Case.class);
                registerDemoStorage(FormInstance.STORAGE_KEY, FormInstance.class);


                registerWrappedDemoStorage(TransportMessageStore.Q_STORENAME, new TransportMessageSerializationWrapper());
                registerWrappedDemoStorage(TransportMessageStore.RECENTLY_SENT_STORENAME, new TransportMessageSerializationWrapper());
                TransportService.reinit();
            } else {
                StorageManager.registerStorage(Case.STORAGE_KEY, Case.class);
                StorageManager.registerStorage(FormInstance.STORAGE_KEY, FormInstance.class);
                StorageManager.registerWrappedStorage(TransportMessageStore.Q_STORENAME, TransportMessageStore.Q_STORENAME, new TransportMessageSerializationWrapper());
                StorageManager.registerWrappedStorage(TransportMessageStore.RECENTLY_SENT_STORENAME, TransportMessageStore.RECENTLY_SENT_STORENAME, new TransportMessageSerializationWrapper());
                TransportService.reinit();
            }
        }
    }

    public void resetDemoData() {

        boolean curmode = inDemoMode;
        if(!inDemoMode) {
            toggleDemoMode(true);
        }

        StorageManager.getStorage(Case.STORAGE_KEY).removeAll();
        StorageManager.getStorage(FormInstance.STORAGE_KEY).removeAll();
        StorageManager.getStorage(TransportMessageStore.Q_STORENAME).removeAll();
        StorageManager.getStorage(TransportMessageStore.RECENTLY_SENT_STORENAME).removeAll();

        toggleDemoMode(curmode);
    }

    public boolean inDemoMode() {
        return inDemoMode;
    }

    public void purgeScheduler (boolean force) {
        int purgeFreq = CommCareProperties.parsePurgeFreq(PropertyManager._().getSingularProperty(CommCareProperties.PURGE_FREQ));
        Date purgeLast = CommCareProperties.parseLastPurge(PropertyManager._().getSingularProperty(CommCareProperties.PURGE_LAST));

        if (force || purgeFreq <= 0 || purgeLast == null || ((new Date().getTime() - purgeLast.getTime()) / 86400000l) >= purgeFreq) {
            String logMsg = purgeMsg(autoPurge());
            PropertyManager._().setProperty(CommCareProperties.PURGE_LAST, DateUtils.formatDateTime(new Date(), DateUtils.FORMAT_ISO8601));
            Logger.log("record-purge", logMsg);
        }
    }

    public Hashtable<String, Hashtable<Integer, String>> autoPurge () {
        Hashtable<String, Hashtable<Integer, String>> deletedLog = new Hashtable<String, Hashtable<Integer, String>>();

        //attempt to purge different types of objects in such an order that, if interrupted, we'll avoid referential integrity errors

        //1) tx queue is self-managing
        //do nothing

        //2) saved forms (keep forms not yet recorded; sent/unsent status should matter in future, but not now, because new tx layer is naive)
        purgeRMS(FormInstance.STORAGE_KEY,
            new EntityFilter<FormInstance> () {
                EntityFilter<FormInstance> antiFilter = new RecentFormFilter();

                //do the opposite of the recent form filter; i.e., if form shows up in the 'unrecorded forms' list, it is NOT safe to delete
                public int preFilter (int id, Hashtable metaData) {
                    int prefilter = antiFilter.preFilter(id, metaData);
                    if(prefilter == EntityFilter.PREFILTER_FILTER) {
                        return EntityFilter.PREFILTER_FILTER;
                    }
                    return  prefilter == EntityFilter.PREFILTER_INCLUDE ?
                            EntityFilter.PREFILTER_EXCLUDE : EntityFilter.PREFILTER_INCLUDE;
                }

                public boolean matches(FormInstance sf) {
                    return !antiFilter.matches(sf);
                }
            }, deletedLog);


        //3) cases (delete cases that are closed AND have no open cases which index them)
        purgeRMS(Case.STORAGE_KEY, caseFilter(), deletedLog);

        //4) Ledger models (ledger database objects with no matching case)
        purgeRMS(Ledger.STORAGE_KEY, new LedgerPurgeFilter((IStorageUtilityIndexed)StorageManager.getStorage(Ledger.STORAGE_KEY),
                (IStorageUtilityIndexed)StorageManager.getStorage(Case.STORAGE_KEY)), deletedLog);

        //5) reclog will never grow that large in size
        //do nothing

        //6) incident log is (mostly) self-managing
        //do nothing

        return deletedLog;
    }

    private EntityFilter<Case> caseFilter() {
        //We need to determine if we're using ownership for purging. For right now, only in sync mode
        Vector<String> owners = null;
        if(CommCareProperties.TETHER_SYNC.equals(PropertyManager._().getSingularProperty(CommCareProperties.TETHER_MODE))) {
            owners = new Vector<String>();
            Vector<String> users = new Vector<String>();
            for(IStorageIterator<User> userIterator = StorageManager.getStorage(User.STORAGE_KEY).iterate(); userIterator.hasMore();) {
                String id = userIterator.nextRecord().getUniqueId();
                owners.addElement(id);
                users.addElement(id);
            }

            //Now add all of the relevant groups
            //TODO: Wow. This is.... kind of megasketch
            for(String userId : users) {
                DataInstance instance = CommCareUtil.loadFixtureForUser("user-groups", userId);
                if(instance == null) { continue; }
                EvaluationContext ec = new EvaluationContext(instance);
                for(TreeReference ref : ec.expandReference(XPathReference.getPathExpr("/groups/group/@id").getReference())) {
                    AbstractTreeElement<AbstractTreeElement> idelement = ec.resolveReference(ref);
                    if(idelement.getValue() != null) {
                        owners.addElement(idelement.getValue().uncast().getString());
                    }
                }
            }
        }


        return new CasePurgeFilter((RMSStorageUtilityIndexed<Case>)StorageManager.getStorage(Case.STORAGE_KEY), owners);

    }

    private void purgeRMS (String key, EntityFilter filt, Hashtable<String, Hashtable<Integer, String>> deletedLog) {
        RMSStorageUtility rms = (RMSStorageUtility)StorageManager.getStorage(key);
        //TODO: Reimplement the printout here.
        //Hashtable<Integer, RMSRecordLoc> index = rms.getIDIndexRecord();

        Vector<Integer> deletedIDs = rms.removeAll(filt);

        Hashtable<Integer, String> deletedDetail = new Hashtable<Integer, String>();
        for (int i = 0; i < deletedIDs.size(); i++) {
            int id = deletedIDs.elementAt(i).intValue();
            //RMSRecordLoc detail = index.get(new Integer(id));
            RMSRecordLoc detail = null;
            deletedDetail.put(new Integer(id), detail != null ? "(" + detail.rmsID + "," + detail.recID + ")" : "?");
        }
        deletedLog.put(key, deletedDetail);
    }

    //aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    private String purgeMsg (Hashtable<String, Hashtable<Integer, String>> detail) {
        if (detail == null)
            return "";

        StringBuffer sb = new StringBuffer();
        int i = 0;
        for (Enumeration e = detail.keys(); e.hasMoreElements(); i++) {
            String key = (String)e.nextElement();
            Hashtable<Integer, String> rmsDetail = detail.get(key);
            sb.append(key + "[");
            int j = 0;
            for (Enumeration f = rmsDetail.keys(); f.hasMoreElements(); j++) {
                int id = ((Integer)f.nextElement()).intValue();
                String ext = rmsDetail.get(new Integer(id));
                sb.append(id + (ext != null ? ":" + ext : ""));
                if (j < rmsDetail.size() - 1)
                    sb.append(",");
            }
            sb.append("]");
            if (i < detail.size() - 1)
                sb.append(",");
        }
        return sb.toString();
    }


    public static final String STORAGE_TABLE_GLOBAL = "GLOBAL_RESOURCE_TABLE";
    private static final String STORAGE_KEY_TEMPORARY = "RESOURCE_TABLE_";

    private static ResourceTable global;

    /**
     * @return A static resource table which
     */
    public static ResourceTable RetrieveGlobalResourceTable() {
        if(global == null) {
            global = ResourceTable.RetrieveTable((IStorageUtilityIndexed)StorageManager.getStorage(STORAGE_TABLE_GLOBAL));
        }
        //Not sure if this reference is actually a good idea, or whether we should
        //get the storage link every time... For now, we'll reload storage each time
        return global;
    }

    public static ResourceTable CreateTemporaryResourceTable(String name) {
        ResourceTable table = new ResourceTable();
        IStorageUtilityIndexed storage = null;
        String storageKey = STORAGE_KEY_TEMPORARY + name.toUpperCase();

        //Check if this table already exists, and return it if so.
        for(String utilityName : StorageManager.listRegisteredUtilities()) {
            if(utilityName.equals(storageKey)) {
                table = ResourceTable.RetrieveTable((IStorageUtilityIndexed)StorageManager.getStorage(storageKey));
            }
        }
        //Otherwise, create a new one.
        if(storage == null) {
            StorageManager.registerStorage(storageKey, storageKey, Resource.class);
            table = ResourceTable.RetrieveTable((IStorageUtilityIndexed)StorageManager.getStorage(storageKey));
        }
        return table;
    }

    /**
     * Clear out anything which may have been left around in the temporary update table
     *
     * @return
     */
    protected static void ClearUpdateTable() {
        //TODO: This is a lot of terrible coupling with the above...
        CreateTemporaryResourceTable(CommCareUpgradeState.UPGRADE_TABLE_NAME).clear();
    }

    public void exitApp() {
        midlet.notifyDestroyed();
    }

    public String getOTAURL() {
        String url = PropertyManager._().getSingularProperty(CommCareProperties.OTA_RESTORE_URL);
        String testingURL = PropertyManager._().getSingularProperty(CommCareProperties.OTA_RESTORE_TEST_URL);
        if(CommCareUtil.isTestingMode() && testingURL != null) {
            url = testingURL;
        }
        return url;
    }

    //custom code for BHOMA -- don't tell anyone
    class CHWReferralNumFunc implements IFunctionHandler {
        Hashtable<String, String> referralCache = new Hashtable<String, String>();

        public String getName() {
            return "chw-referral-num";
        }

        public Vector getPrototypes() {
            Vector p = new Vector();
            p.addElement(new Class[] {String.class});
            return p;
        }

        public boolean rawArgs() {
            return false;
        }

        public boolean realTime() {
            return false;
        }

        public Object eval(Object[] args, EvaluationContext ec) {
            //each repeat has a hidden generated uid. we cache the generated referral code
            //under this uid, so we don't regenerate it if we navigate through the repeititon
            //again
            String key = (String)args[0];

            if (key.length() == 0) {
                //referral code is non-relevant; don't generate/increment
                return "_nonrelev";
            } else if (referralCache.containsKey(key)) {
                //fetch cached referral code
                return referralCache.get(key);
            } else {
                //generate/increment fresh referral code and cache it
                User u = CommCareContext._().getUser();
                String refCode = u.getProperty("clinic_prefix") + "-" + u.getProperty("chw_zone") + "-";

                String sRefCounter = u.getProperty("ref_count");
                int refCounter = (sRefCounter == null ? 0 : Integer.parseInt(sRefCounter));

                refCounter += 1;
                if (refCounter >= 10000)
                    refCounter = 1;
                sRefCounter = Integer.toString(refCounter);
                u.setProperty("ref_count", sRefCounter);

                IStorageUtility users = StorageManager.getStorage(User.STORAGE_KEY);
                try {
                    users.write(u);
                } catch (StorageFullException e) {
                    Logger.exception(e);
                }

                while (sRefCounter.length() < 4) {
                    sRefCounter = "0" + sRefCounter;
                }
                refCode += sRefCounter;

                referralCache.put(key, refCode);
                return refCode;
            }
        }
    }
}
