/**
 * 
 */
package org.commcare.util;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.midlet.MIDlet;

import org.commcare.core.properties.CommCareProperties;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.services.AutomatedSenderService;
import org.commcare.view.CommCareStartupInteraction;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.cases.CaseManagementModule;
import org.javarosa.cases.model.Case;
import org.javarosa.cases.util.CasePreloadHandler;
import org.javarosa.chsreferral.PatientReferralModule;
import org.javarosa.chsreferral.model.PatientReferral;
import org.javarosa.chsreferral.util.PatientReferralPreloader;
import org.javarosa.core.model.CoreModelModule;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.services.transport.payload.IDataPayload;
import org.javarosa.core.util.JavaRosaCoreModule;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.formmanager.FormManagerModule;
import org.javarosa.formmanager.properties.FormManagerProperties;
import org.javarosa.j2me.J2MEModule;
import org.javarosa.j2me.storage.rms.RMSRecordLoc;
import org.javarosa.j2me.storage.rms.RMSStorageUtility;
import org.javarosa.j2me.storage.rms.RMSTransaction;
import org.javarosa.j2me.util.DumpRMS;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.log.LogManagementModule;
import org.javarosa.log.util.LogReportUtils;
import org.javarosa.model.xform.XFormsModule;
import org.javarosa.resources.locale.LanguagePackModule;
import org.javarosa.resources.locale.LanguageUtils;
import org.javarosa.services.transport.TransportManagerModule;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;
import org.javarosa.user.activity.UserModule;
import org.javarosa.user.model.User;
import org.javarosa.user.utility.UserPreloadHandler;
import org.javarosa.user.utility.UserUtility;
import org.javarosa.xform.util.XFormUtils;

import de.enough.polish.ui.Display;

/**
 * @author ctsims
 *
 */
public class CommCareContext {

	private static CommCareContext i;
	
	private MIDlet midlet;
	private User user;
	
	private CommCarePlatform manager;
	
	protected boolean inDemoMode;
	
	public String getSubmitURL() {
		String url = PropertyManager._().getSingularProperty(CommCareProperties.POST_URL_PROPERTY);
		
		String testUrl = PropertyManager._().getSingularProperty(CommCareProperties.POST_URL_TEST_PROPERTY);
		if(CommCareUtil.isTestingMode() && testUrl != null) {
			//In testing mode, use this URL instead, if available.
			url = testUrl;
		}
		return url;
	}
	
	public TransportMessage buildMessage(IDataPayload payload) {
		//Right now we have to just give the message the stream, rather than the payload,
		//since the transport layer won't take payloads. This should be fixed _as soon 
		//as possible_ so that we don't either (A) blow up the memory or (B) lose the ability
		//to send payloads > than the phones' heap.
		
		String url = getSubmitURL();
		try {
			return new SimpleHttpTransportMessage(payload.getPayloadStream(), url);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error Serializing Data to be transported");
		}
	}
	
	public MIDlet getMidlet() {
		return midlet;
	}
	
	public CommCarePlatform getManager() {
		return manager;
	}
	
	public void configureApp(MIDlet m, InitializationListener listener) {
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
			protected void runWrapper() throws UnfullfilledRequirementsException {
				
				manager = new CommCarePlatform(CommCareUtil.getMajorVersion(), CommCareUtil.getMinorVersion());
				
				//Try to initialize and install the application resources...
				try {
					ResourceTable global = RetrieveGlobalResourceTable();
					if(global.isEmpty()) {
						this.setMessage(CommCareStartupInteraction.failSafeText("commcare.firstload","First start detected, loading resources..."));
					}
					manager.init(CommCareUtil.getProfileReference(), global, false);
				} catch (UnfullfilledRequirementsException e) {
					if(e.getSeverity() == UnfullfilledRequirementsException.SEVERITY_PROMPT) {
						String message = e.getMessage();
						if(e.getRequirementCode() == UnfullfilledRequirementsException.REQUIREMENT_MAJOR_APP_VERSION || e.getRequirementCode() == UnfullfilledRequirementsException.REQUIREMENT_MAJOR_APP_VERSION) {
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
					throw new RuntimeException(e.getMessage());
				}
				
				//When we might initialize language files, we need to make sure it's not trying
				//to load any of them into memory, since the default ones are not guaranteed to
				//be added later.
				Localization.setLocale("default");
				manager.initialize(RetrieveGlobalResourceTable());
				
				purgeScheduler();
				
				//Now that the profile has had a chance to set properties (without them requiring
				//override) set the fallback defaults.
				postProfilePropertyInit();
				
				initUserFramework();
				
				//Establish default logging deadlines
				LogReportUtils.initPendingDates(new Date().getTime());
				
				//Now we can initialize the language for real.
				LanguageUtils.initializeLanguage(true,"default");
				
				//We need to let All Localizations register before we can do this
				J2MEDisplay.init(CommCareContext.this.midlet);
				
				if(CommCareProperties.SEND_UNSENT_AUTOMATIC.equals(PropertyManager._().getSingularProperty(CommCareProperties.SEND_UNSENT_STYLE))) {
					AutomatedSenderService.InitializeAndSpawnSenderService();
				}
			}

			protected void askForResponse(String message, YesNoListener yesNoListener) {
				interaction.AskYesNo(message,yesNoListener);
			}

			protected void setMessage(String message) {
				interaction.setMessage(message);
			}
		};
		
		initializer.initialize(listener);
	}

	private void failsafeInit (MIDlet m) {
		DumpRMS.RMSRecoveryHook(m);
		new J2MEModule().registerModule();
	}
	
	private void initUserFramework() {
		if(manager.getCurrentProfile().isFeatureActive("users")) {
			UserUtility.populateAdminUser();
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (StorageFullException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		}
	}
	
	protected void registerAddtlStorage () {
		//do nothing
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
		new PatientReferralModule().registerModule();
		new CoreModelModule().registerModule();
		new XFormsModule().registerModule();
		new CaseManagementModule().registerModule();
		new TransportManagerModule().registerModule();
		new CommCareModule().registerModule();
		new FormManagerModule().registerModule();
	}
	
	protected void setProperties() {
		
		//NOTE: These properties should all be properties which are not expected to
		//be set by the profile, otherwise the profile will need to override the existing property.
		//Put generic fallbacks into postProfile property intiializer below.
		PropertyManager._().addRules(new JavaRosaPropertyRules());
		PropertyManager._().addRules(new CommCareProperties());
		PropertyManager._().addRules(new FormManagerProperties());
		PropertyUtils.initializeProperty("DeviceID", PropertyUtils.genGUID(25));
		
		PropertyUtils.initializeProperty(CommCareProperties.IS_FIRST_RUN, CommCareProperties.FIRST_RUN_YES);
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
	}
	
	public static void init(MIDlet m, InitializationListener listener) {
		i = new CommCareContext();
		i.configureApp(m, listener);
	}
	
	public static CommCareContext _() {
		if(i == null) {
			throw new RuntimeException("CommCareContext must be initialized with the Midlet to be used.");
		}
		return i;
	}

	public void setUser (User u) {
		this.user = u;
	}
	
	public User getUser () {
		return user;
	}
	
	
	public Vector<IFunctionHandler> getFuncHandlers () {
		Vector<IFunctionHandler> handlers = new Vector<IFunctionHandler>();
		handlers.addElement(new HouseholdExistsFuncHandler());
		handlers.addElement(new CHWReferralNumFunc()); //BHOMA custom!
		return handlers;
	}
	
	/// Probably put this stuff into app specific ones.
	public Vector<IPreloadHandler> getPreloaders() {
		return getPreloaders(null, null);
	}
	
	public Vector<IPreloadHandler> getPreloaders(PatientReferral r) {
		Case c = CommCareUtil.getCase(r.getLinkedId());
		return getPreloaders(c, r);
	}
	
	public Vector<IPreloadHandler> getPreloaders(Case c) {
		return getPreloaders(c, null);
	}
	
	public Vector<IPreloadHandler> getPreloaders(Case c, PatientReferral r) {
		Vector<IPreloadHandler> handlers = new Vector<IPreloadHandler>();
		if(c != null) {
			CasePreloadHandler p = new CasePreloadHandler(c);
			handlers.addElement(p);
		}
		if(r != null) {
			PatientReferralPreloader rp = new PatientReferralPreloader(r);
			handlers.addElement(rp);
		}
		MetaPreloadHandler meta = new MetaPreloadHandler(this.getUser());
		handlers.addElement(meta);
		handlers.addElement(new UserPreloadHandler(this.getUser()));
		return handlers;		
	}
	
	private void registerDemoStorage (String key, Class type) {
		StorageManager.registerStorage(key, "DEMO_" + key, type);
	}
	
	public void toggleDemoMode(boolean demoOn) {
		if (demoOn != inDemoMode) {
			inDemoMode = demoOn;
			if (demoOn) {
				registerDemoStorage(Case.STORAGE_KEY, Case.class);
				registerDemoStorage(PatientReferral.STORAGE_KEY, PatientReferral.class);
				registerDemoStorage(FormInstance.STORAGE_KEY, FormInstance.class);
				//TODO: Use new transport message queue
			} else {
				StorageManager.registerStorage(Case.STORAGE_KEY, Case.class);
				StorageManager.registerStorage(PatientReferral.STORAGE_KEY, PatientReferral.class);
				StorageManager.registerStorage(FormInstance.STORAGE_KEY, FormInstance.class);
				//TODO: Use new transport message queue
			}
		}
	}
	
	public void resetDemoData() {
		//#debug debug
		System.out.println("Resetting demo data");
	
		StorageManager.getStorage(Case.STORAGE_KEY).removeAll();
		StorageManager.getStorage(PatientReferral.STORAGE_KEY).removeAll();
		StorageManager.getStorage(FormInstance.STORAGE_KEY).removeAll();
		//TODO: Use new transport message queue
	}

	public void purgeScheduler () {
		int purgeFreq = CommCareProperties.parsePurgeFreq(PropertyManager._().getSingularProperty(CommCareProperties.PURGE_FREQ));
		Date purgeLast = CommCareProperties.parseLastPurge(PropertyManager._().getSingularProperty(CommCareProperties.PURGE_LAST));
		
		if (purgeFreq <= 0 || purgeLast == null || ((new Date().getTime() - purgeLast.getTime()) / 86400000l) >= purgeFreq) {
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
		
		//3) referrals (keep only pending referrals)
		final Vector<String> casesWithActiveReferrals = new Vector<String>();
		purgeRMS(PatientReferral.STORAGE_KEY,
			new EntityFilter<PatientReferral> () {
				public boolean matches(PatientReferral r) {
					if (r.isPending()) {
						String caseID = r.getLinkedId();
						if (!casesWithActiveReferrals.contains(caseID)) {
							casesWithActiveReferrals.addElement(caseID);
						}
						return false;
					} else {
						return true;
					}
				}
			}, deletedLog);
		
		//4) cases (delete cases that are closed AND have no open referrals pointing to them
		//          AND have no unrecorded forms)		
		purgeRMS(Case.STORAGE_KEY,
			new EntityFilter<Case> () {
				public boolean matches(Case c) {
					return c.isClosed() && !casesWithActiveReferrals.contains(c.getCaseId());
				}
			}, deletedLog);

		//5) reclog will never grow that large in size
		//do nothing
		
		//6) incident log is (mostly) self-managing
		//do nothing
		
		return deletedLog;
	}
	
	private void purgeRMS (String key, EntityFilter filt, Hashtable<String, Hashtable<Integer, String>> deletedLog) {
		RMSStorageUtility rms = (RMSStorageUtility)StorageManager.getStorage(key);
		Hashtable<Integer, RMSRecordLoc> index = rms.getIDIndexRecord();
		
		Vector<Integer> deletedIDs = rms.removeAll(filt);
		
		Hashtable<Integer, String> deletedDetail = new Hashtable<Integer, String>();
		for (int i = 0; i < deletedIDs.size(); i++) {
			int id = deletedIDs.elementAt(i).intValue();
			RMSRecordLoc detail = index.get(new Integer(id));
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
		System.out.println("Global Resource Table");
		System.out.println(global);
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
		System.out.println("Temporary Resource Table");
		System.out.println(table);
		return table;
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
		public String getName() {
			return "chw-referral-num";
		}

		public Vector getPrototypes() {
			Vector p = new Vector();
			p.addElement(new Class[] {});
			return p;
		}

		public boolean rawArgs() {
			return false;
		}

		public boolean realTime() {
			return false;
		}

		public Object eval(Object[] args) {
			User u = CommCareContext._().getUser();
			String refCode = u.getProperty("clinic_prefix") + "-" + u.getProperty("chw_zone") + "-";
			
			String sRefCounter = u.getProperty("ref_count");
			int refCounter = (sRefCounter == null ? 0 : Integer.parseInt(sRefCounter));

			refCounter += 1;
			if (refCounter >= 1000)
				refCounter = 1;
			sRefCounter = Integer.toString(refCounter);
			u.setProperty("ref_count", sRefCounter);
			
			IStorageUtility users = StorageManager.getStorage(User.STORAGE_KEY);
			try {
				users.write(u);
			} catch (StorageFullException e) {
				Logger.exception(e);
			}
			
			while (sRefCounter.length() < 3) {
				sRefCounter = "0" + sRefCounter;
			}
			refCode += sRefCounter;
			
			return refCode;
		}
	}
}
