/**
 * 
 */
package org.commcare.util;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.applogic.CommCareAlertState;
import org.commcare.applogic.CommCareFirstStartState;
import org.commcare.applogic.CommCareHomeState;
import org.commcare.applogic.CommCareLoginState;
import org.commcare.core.properties.CommCareProperties;
import org.commcare.entity.CaseInstanceLoader;
import org.commcare.entity.CommCareEntity;
import org.commcare.entity.ReferralInstanceLoader;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Suite;
import org.javarosa.cases.model.Case;
import org.javarosa.chsreferral.model.PatientReferral;
import org.javarosa.chsreferral.util.IPatientReferralFilter;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.entity.model.Entity;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.formmanager.api.JrFormEntryController;
import org.javarosa.formmanager.api.JrFormEntryModel;
import org.javarosa.formmanager.properties.FormManagerProperties;
import org.javarosa.formmanager.utility.FormDefFetcher;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.service.transport.securehttp.HttpCredentialProvider;
import org.javarosa.services.transport.TransportService;

/**
 * @author ctsims
 *
 */
public class CommCareUtil {
	
	
	private static final String COMMCARE_RELEASE_PROPERTY = "CommCare-Release";
	private final static String PROP_APP_VERSION = "App-Version";
	private final static String PROP_CC_APP_VERSION = "CommCare-Version";
	private final static String PROP_JR_BUILD_VERSION = "JR-Build-Version";
	private final static String PROP_CC_BUILD_VERSION = "CC-Build-Version";
	private final static String PROP_CONTENT_VERSION = "CC-Content-Version";
	private final static String PROP_POLISH_VERSION = "Polish-Version";
	private final static String PROP_POLISH_DEVICE = "Polish-Device";
	private final static String PROP_BUILD_DATE = "Built-on";
	private final static String PROP_RELEASE_DATE = "Released-on";
	private final static String PROP_BUILD_NUM = "Build-Number";
	private final static String PROP_PROFILE_REFERENCE = "Profile";
	
	public final static int VERSION_SHORT = 1;
	public final static int VERSION_MED = 2;
	public final static int VERSION_LONG = 3;
	
	public static int getNumberUnsent() {
		return TransportService.getCachedMessagesSize();
	}
	
	public static String getProfileReference() {
		return getAppProperty(PROP_PROFILE_REFERENCE);
	}
		
	public static String getAppProperty (String key) {
		return CommCareContext._().getMidlet().getAppProperty(key);
	}

	public static String getAppProperty (String key, String defaultValue) {
		String prop = getAppProperty(key);
		return (prop != null ? prop : defaultValue);
	}
	
	public static String getVersion () {
		return getVersion(VERSION_LONG);
	}
	
	public static String getVersion (int type) {
		final int hashLength = 6;
		String vApp = getAppProperty(PROP_APP_VERSION, "??");
		String vHumanApp = getAppProperty(PROP_CC_APP_VERSION, vApp);
		String vBuildJR = getAppProperty(PROP_JR_BUILD_VERSION, "??");
		String vBuildCC = getAppProperty(PROP_CC_BUILD_VERSION, "??");
		String vContent = getAppProperty(PROP_CONTENT_VERSION, "??");
		String vPolish = getAppProperty(PROP_POLISH_VERSION, "??");
		String vDevice = getAppProperty(PROP_POLISH_DEVICE, "??");
		String buildDate = getAppProperty(PROP_BUILD_DATE, "??");
		String releaseDate = getAppProperty(PROP_RELEASE_DATE, "--");
		String buildNum = getAppProperty(PROP_BUILD_NUM, "custom");
		boolean released = !isTestingMode();
		
		vBuildJR = PropertyUtils.trim(vBuildJR, hashLength);
		vBuildCC = PropertyUtils.trim(vBuildCC, hashLength);
		vContent = PropertyUtils.trim(vContent, hashLength);
		vPolish = (String)DateUtils.split(vPolish, " ", true).elementAt(0);
		buildDate = (String)DateUtils.split(buildDate, " ", true).elementAt(0);
		releaseDate = (String)DateUtils.split(releaseDate, " ", true).elementAt(0);
		
		switch (type) {
		case VERSION_LONG:
			return vHumanApp + " (" + vBuildJR + "-" + vBuildCC + "-" + vContent + "-" + vPolish + "-" + vDevice +
				")" + (released ? " #" + buildNum : "") + " b:" + buildDate + " r:" + releaseDate;
		case VERSION_MED:
			return vHumanApp + " " + "#" + buildNum + (released ? " (" + releaseDate + ")" : "<unreleased>");
		case VERSION_SHORT:
			return vHumanApp;
		default: throw new RuntimeException("unknown version type");
		}
	}
		
	public static IPatientReferralFilter overdueFilter(final String caseType) {
		return new IPatientReferralFilter() {
			public boolean inFilter(PatientReferral ref) {
				Date due = ref.getDateDue();		
				String caseTypeId = getCase(ref.getLinkedId()).getTypeId();
				return (due != null && DateUtils.dateDiff(due, DateUtils.today()) >= 0 &&
						(caseType == null || caseTypeId.equals(caseType)));
			}
		};
	}
	
	public static Case getCase (int recordId) {
		IStorageUtility cases = StorageManager.getStorage(Case.STORAGE_KEY);
		return (Case)cases.read(recordId);
	}
	
	public static Case getCase (String caseId) {
		IStorageUtilityIndexed cases = (IStorageUtilityIndexed)StorageManager.getStorage(Case.STORAGE_KEY);
		return (Case)cases.getRecordForValue("case-id", caseId);
	}
	 
	public static PatientReferral getReferral (int id) {
		IStorageUtility referrals = StorageManager.getStorage(PatientReferral.STORAGE_KEY);
		return (PatientReferral)referrals.read(id);
	}
	
	public static PatientReferral getReferral (String referralId, String type) {
		IStorageUtilityIndexed referrals = (IStorageUtilityIndexed)StorageManager.getStorage(PatientReferral.STORAGE_KEY);
		for(Integer id : (Vector<Integer>)referrals.getIDsForValue("referral-id", referralId)) {
			PatientReferral ref = (PatientReferral)referrals.read(id.intValue());
			if(ref.getType().equals(type)) {
				return ref;
			}
		}
		return null;
	}
	 
	
	public static FormDef getForm (int id) {
		IStorageUtility forms = StorageManager.getStorage(FormDef.STORAGE_KEY);
		return (FormDef)forms.read(id);
	}
		
	public static boolean isTestingMode() {
		String mode = PropertyManager._().getSingularProperty(CommCareProperties.DEPLOYMENT_MODE);
		if (mode == null || mode.equals(CommCareProperties.DEPLOY_DEFAULT)) {
			return !"true".equals(CommCareUtil.getAppProperty(COMMCARE_RELEASE_PROPERTY));
		} else {
			return mode.equals(CommCareProperties.DEPLOY_TESTING);
		}
	}
			
	public static void exit () {
		Logger.log("app-close", "");
		CommCareContext._().getMidlet().notifyDestroyed();
	}
	
	public static void launchHomeState() {
		J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
	}
		
	public static int countEntities(Entry entry, Suite suite) {
		final Entry e = entry;
		
		Hashtable<String, String> references = entry.getReferences();
		if(references.size() == 0) {
			throw new RuntimeException("Attempt to count entities for an entry with no references!");
		}
		else {
			//this will be revisited and rewritten 
			boolean referral = false;
			int count = 0;
			// Need to do some reference gathering... 
			for(Enumeration en = references.keys() ; en.hasMoreElements() ; ) {
				String key = (String)en.nextElement();
				String refType = references.get(key);
				if(refType.toLowerCase().equals("referral")) {
					referral = true;
				}
			}
			if(referral) {
				Entity<PatientReferral> entity = new CommCareEntity<PatientReferral>(suite.getDetail(entry.getShortDetailId()), suite.getDetail(entry.getLongDetailId()), new ReferralInstanceLoader(e.getReferences()));
				EntityFilter<? super PatientReferral> filter = entity.getFilter();
				for(IStorageIterator i = StorageManager.getStorage(PatientReferral.STORAGE_KEY).iterate(); i.hasMore() ;) {
					if(filter.matches((PatientReferral)i.nextRecord())) {
						count++;
					}
				}
				
				return count;
			} else {
				Entity<Case> entity = new CommCareEntity<Case>(suite.getDetail(entry.getShortDetailId()), suite.getDetail(entry.getLongDetailId()), new CaseInstanceLoader(e.getReferences()));
				EntityFilter<? super Case> filter = entity.getFilter();
				for(IStorageIterator i = StorageManager.getStorage(Case.STORAGE_KEY).iterate(); i.hasMore() ;) {
					if(filter.matches((Case)i.nextRecord())) {
						count++;
					}
				}
				
				return count;
			}
		}
	}
	
	private static int[] getVersions() {
		try {
			String vApp = getAppProperty(PROP_APP_VERSION, "blank");
			if ("blank".equals(vApp)) {
				return null;
			}

			Vector<String> split = DateUtils.split(vApp, ".", false);
			if (split.size() < 2) {
				return null;
			}

			int[] version = new int[split.size()];
			for (int i = 0; i < version.length; ++i) {
				version[i] = Integer.parseInt(split.elementAt(i));
			}
			return version;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static int getMajorVersion() {
		int[] versions = getVersions();
		if(versions != null) {
			return versions[0];
		} else {
			return -1;
		}
	}
	
	public static int getMinorVersion() {
		int[] versions = getVersions();
		if(versions != null) {
			return versions[1];
		} else {
			return -1;
		}
	}
	
	public static void launchFirstState() {
		if(!CommCareContext._().getManager().getCurrentProfile().isFeatureActive("users")) {
			CommCareUtil.launchHomeState();
			return;
		}
		if(CommCareProperties.FIRST_RUN_YES.equals(PropertyManager._().getSingularProperty(CommCareProperties.IS_FIRST_RUN))) {
			J2MEDisplay.startStateWithLoadingScreen(new CommCareFirstStartState());
		} else {
			J2MEDisplay.startStateWithLoadingScreen(new CommCareLoginState());
		}
	}

	public static void exitMain() {
		if(CommCareContext._().getManager().getCurrentProfile().isFeatureActive("users")) {
			J2MEDisplay.startStateWithLoadingScreen(new CommCareLoginState());
		} else{
			exit();
		}
	}

	public static JrFormEntryController createFormEntryController(FormDefFetcher fetcher) {
		return createFormEntryController(new JrFormEntryModel(fetcher.getFormDef()));
	}	
	
	public static JrFormEntryController createFormEntryController(JrFormEntryModel model) {
		return new JrFormEntryController(model, CommCareSense.formEntryExtraKey(), true, CommCareSense.formEntryQuick());
	}

	
	/**
	 * Gets the text associated with this entry, while dynamically evaluating
	 * and resolving any necessary count arguments that might need to be 
	 * included. 
	 * 
	 * @param entry
	 * @return
	 */
	public static String getEntryText(Entry entry, Suite suite) {
		String text = entry.getText().evaluate();
		if(Localizer.getArgs(text).size() == 0) {
			return text;
		}
		else if(Localizer.getArgs(text).size() > 1) {
			//We really don't know how to deal with this yet. Shouldn't happen!
			return text;
		} else {
			//Sweet spot! This argument should be the count of all entities
			//which are possible inside of its selection.
			String wrapper = Localization.get("commcare.numwrapper"); 
			String wrapped = Localizer.processArguments(wrapper, new String[] { String.valueOf(CommCareUtil.countEntities(entry, suite)) });
			return Localizer.processArguments(text, new String[] {wrapped} );
		}
	}

	public static CommCareAlertState alertFactory (String title, String content) {
		return new CommCareAlertState(title, content) {
			public void done() {
				J2MEDisplay.startStateWithLoadingScreen(new CommCareHomeState());
			}
		};
	}
	
	public static boolean demoEnabled() {
		return !CommCareProperties.DEMO_DISABLED.equals(PropertyManager._().getSingularProperty(CommCareProperties.DEMO_MODE));
	}
	
	public static boolean partialRestoreEnabled() {
		return !CommCareProperties.REST_TOL_STRICT.equals(PropertyManager._().getSingularProperty(CommCareProperties.RESTORE_TOLERANCE));
	}

	public static HttpCredentialProvider wrapCredentialProvider(HttpCredentialProvider credentialProvider) {
		String domain = PropertyManager._().getSingularProperty(CommCareProperties.USER_DOMAIN);
		if(domain == null || domain == "") {
			return credentialProvider;
		} else {
			return new CommCareUserCredentialProvider(credentialProvider, domain);
		}
	}
}
