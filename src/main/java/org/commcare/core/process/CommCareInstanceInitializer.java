package org.commcare.core.process;

import org.commcare.cases.instance.CaseDataInstance;
import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.instance.LedgerInstanceTreeElement;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.sandbox.SandboxUtils;
import org.commcare.session.SessionFrame;
import org.commcare.session.SessionInstanceBuilder;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.User;
import org.commcare.session.CommCareSession;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.CacheTable;

/**
 * Initializes a CommCare DataInstance against a UserDataInterface and (sometimes) optional
 * CommCareSession/Platform
 *
 * @author ctsims
 * @author wspride
 */
public class CommCareInstanceInitializer extends InstanceInitializationFactory {

    protected final CommCareSession session;
    protected CaseInstanceTreeElement casebase;
    protected LedgerInstanceTreeElement stockbase;
    private final CacheTable<String, TreeElement> fixtureBases = new CacheTable<>();
    protected final UserSandbox mSandbox;
    protected final CommCarePlatform mPlatform;


    // default constructor because Jython is annoying
    public CommCareInstanceInitializer() {
        this(null, null, null);
    }

    public CommCareInstanceInitializer(UserSandbox sandbox) {
        this(null, sandbox, null);
    }

    public CommCareInstanceInitializer(UserSandbox sandbox, CommCarePlatform platform) {
        this(null, sandbox, platform);
    }

    public CommCareInstanceInitializer(UserSandbox sandbox, CommCareSession session) {
        this(session, sandbox, null);
    }

    public CommCareInstanceInitializer(CommCareSession session, UserSandbox sandbox, CommCarePlatform platform) {
        this.session = session;
        this.mSandbox = sandbox;
        this.mPlatform = platform;
    }

    @Override
    public ExternalDataInstance getSpecializedExternalDataInstance(ExternalDataInstance instance) {
        if (instance.useCaseTemplate()) {
            return new CaseDataInstance(instance);
        } else {
            return instance;
        }
    }

    @Override
    public AbstractTreeElement generateRoot(ExternalDataInstance instance) {
        String ref = instance.getReference();
        if (ref.contains(LedgerInstanceTreeElement.MODEL_NAME)) {
            return setupLedgerData(instance);
        } else if (ref.contains(CaseInstanceTreeElement.MODEL_NAME)) {
            return setupCaseData(instance);
        } else if (instance.getReference().contains("fixture")) {
            return setupFixtureData(instance);
        } else if (instance.getReference().contains("session")) {
            return setupSessionData(instance);
        } else if (instance.getReference().startsWith(ExternalDataInstance.JR_REMOTE_REFERENCE)) {
            return setupRemoteData(instance);
        } else if (ref.contains("migration")) {
            return setupMigrationData(instance);
        }
        return null;
    }

    protected AbstractTreeElement setupLedgerData(ExternalDataInstance instance) {
        if (stockbase == null) {
            stockbase = new LedgerInstanceTreeElement(instance.getBase(), mSandbox.getLedgerStorage());
        } else {
            //re-use the existing model if it exists.
            stockbase.rebase(instance.getBase());
        }
        return stockbase;
    }

    protected AbstractTreeElement setupCaseData(ExternalDataInstance instance) {
        if (casebase == null) {
            casebase = new CaseInstanceTreeElement(instance.getBase(), mSandbox.getCaseStorage());
        } else {
            //re-use the existing model if it exists.
            casebase.rebase(instance.getBase());
        }
        return casebase;
    }


    protected AbstractTreeElement setupFixtureData(ExternalDataInstance instance) {
        return loadFixtureRoot(instance, instance.getReference());
    }

    protected static String getRefId(String reference) {
        return reference.substring(reference.lastIndexOf('/') + 1, reference.length());
    }

    protected TreeElement loadFixtureRoot(ExternalDataInstance instance,
                                          String reference) {
        String refId = getRefId(reference);
        String instanceBase = instance.getBase().getInstanceName();

        String userId = "";
        User u = mSandbox.getLoggedInUser();

        if (u != null) {
            userId = u.getUniqueId();
        }

        try {
            String key = refId + userId + instanceBase;

            TreeElement root = fixtureBases.retrieve(key);
            if (root == null) {
                IStorageUtilityIndexed<FormInstance> fixtureStorage = null;
                if (mPlatform != null) {
                    fixtureStorage = mPlatform.getFixtureStorage();
                }

                FormInstance fixture = SandboxUtils.loadFixture(mSandbox,
                        refId,
                        userId,
                        fixtureStorage);

                if (fixture == null) {
                    throw new FixtureInitializationException(reference);
                }

                root = fixture.getRoot();
                fixtureBases.register(key, root);
            }

            root.setParent(instance.getBase());
            return root;
        } catch (IllegalStateException ise) {
            throw new FixtureInitializationException(reference);
        }
    }

    protected AbstractTreeElement setupSessionData(ExternalDataInstance instance) {
        if (this.mPlatform == null) {
            throw new RuntimeException("Cannot generate session instance with undeclared platform!");
        }
        User u = mSandbox.getLoggedInUserUnsafe();
        TreeElement root =
                SessionInstanceBuilder.getSessionInstance(session.getFrame(), getDeviceId(),
                        getVersionString(), getCurrentDrift(), u.getUsername(), u.getUniqueId(),
                        u.getProperties()).getRoot();
        root.setParent(instance.getBase());
        return root;
    }

    protected long getCurrentDrift() {
        return 0;
    }

    protected AbstractTreeElement setupRemoteData(ExternalDataInstance instance) {
        for (StackFrameStep step : session.getFrame().getSteps()) {
            if (step.getId().equals(instance.getInstanceId()) && step.getType().equals(SessionFrame.STATE_QUERY_REQUEST)) {
                return step.getXmlInstance().getRoot();
            }
        }
        return instance.getRoot();
    }

    protected String getDeviceId() {
        return "----";
    }

    public String getVersionString() {
        return "CommCare Version: " + mPlatform.getMajorVersion() + "." + mPlatform.getMinorVersion();
    }

    protected AbstractTreeElement setupMigrationData(ExternalDataInstance instance) {
        return null;
    }

    public static class FixtureInitializationException extends RuntimeException {

        public FixtureInitializationException(String lookupReference) {
            super(Localization.getWithDefault("lookup.table.missing.error",
                    new String[]{lookupReference},
                    "Unable to find lookup table: " + lookupReference));
        }
    }
}
