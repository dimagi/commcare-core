package org.commcare.core.process;

import static org.javarosa.core.model.instance.ExternalDataInstance.JR_SEARCH_INPUT_REFERENCE;
import static org.javarosa.core.model.instance.ExternalDataInstance.JR_SELECTED_ENTITIES_REFERENCE;

import org.commcare.cases.instance.CaseDataInstance;
import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.instance.LedgerInstanceTreeElement;
import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.interfaces.VirtualDataInstanceStorage;
import org.commcare.core.sandbox.SandboxUtils;
import org.commcare.data.xml.VirtualInstances;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.session.SessionInstanceBuilder;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.ConcreteInstanceRoot;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.InstanceRoot;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.LocalCacheTable;

import javax.annotation.Nonnull;

/**
 * Initializes a CommCare DataInstance against a UserDataInterface and (sometimes) optional
 * CommCareSession/Platform
 *
 * @author ctsims
 * @author wspride
 */
public class CommCareInstanceInitializer extends InstanceInitializationFactory {

    protected final SessionWrapper sessionWrapper;
    protected CaseInstanceTreeElement casebase;
    protected LedgerInstanceTreeElement stockbase;
    private final LocalCacheTable<String, TreeElement> fixtureBases = new LocalCacheTable<>();
    protected final UserSandbox mSandbox;
    protected final CommCarePlatform mPlatform;


    // default constructor because Jython is annoying
    public CommCareInstanceInitializer() {
        this(null, null, null);
    }

    public CommCareInstanceInitializer(UserSandbox sandbox) {
        this(null, sandbox, null);
    }

    public CommCareInstanceInitializer(SessionWrapper sessionWrapper, UserSandbox sandbox,
            CommCarePlatform platform) {
        this.sessionWrapper = sessionWrapper;
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
    @Nonnull
    public InstanceRoot generateRoot(ExternalDataInstance instance) {
        String ref = instance.getReference();
        if (ref.contains(LedgerInstanceTreeElement.MODEL_NAME)) {
            return setupLedgerData(instance);
        } else if (ref.contains(CaseInstanceTreeElement.MODEL_NAME)) {
            return setupCaseData(instance);
        } else if (ref.contains("fixture")) {
            return setupFixtureData(instance);
        } else if (instance.getReference().contains("session")) {
            return setupSessionData(instance);
        } else if (ref.startsWith(ExternalDataInstance.JR_REMOTE_REFERENCE)) {
            return setupExternalDataInstance(instance, ref, SessionFrame.STATE_QUERY_REQUEST);
        } else if (ref.startsWith(JR_SELECTED_ENTITIES_REFERENCE)) {
            return setupSelectedEntitiesInstance(instance, ref);
        } else if (ref.startsWith(JR_SEARCH_INPUT_REFERENCE)) {
            return setupExternalDataInstance(instance, ref, SessionFrame.STATE_QUERY_REQUEST);
        }
        return ConcreteInstanceRoot.NULL;
    }

    private InstanceRoot setupSelectedEntitiesInstance(ExternalDataInstance instance, String ref) {
        String stepType = SessionFrame.STATE_MULTIPLE_DATUM_VAL;
        InstanceRoot instanceRoot = setupExternalDataInstance(instance, ref, stepType);
        if (instanceRoot == ConcreteInstanceRoot.NULL) {
            instanceRoot = getExternalDataInstanceSourceByStepValue(instance, stepType);
        }
        return instanceRoot;
    }

    // Tries to get instance by looking for the instance with id equal to the datum value in the storage
    private InstanceRoot getExternalDataInstanceSourceByStepValue(ExternalDataInstance instance,
            String stepType) {
        RemoteInstanceFetcher instanceFetcher = sessionWrapper.getRemoteInstanceFetcher();
        if (instanceFetcher != null) {
            VirtualDataInstanceStorage instanceStorage = instanceFetcher.getVirtualDataInstanceStorage();
            for (StackFrameStep step : sessionWrapper.getFrame().getSteps()) {
                if (step.getType().equals(stepType)) {
                    try {
                        ExternalDataInstance loadedInstance = instanceStorage.read(step.getValue(),
                                instance.getInstanceId(),
                                instance.getReference());
                        return new ConcreteInstanceRoot(loadedInstance.getRoot());
                    } catch (VirtualInstances.InstanceNotFoundException e) {
                        // continue looping
                    }
                }
            }
        }
        return ConcreteInstanceRoot.NULL;
    }

    /**
     * Initialises instances with reference to 'selected_cases'
     *
     * @param instance  External data Instance that needs to be initialised
     * @param reference instance source reference
     * @param stepType  type of CommCare session frame step with which the given instance is bundled with
     * @return Initialised instance root for the the given instance
     */
    protected InstanceRoot setupExternalDataInstance(ExternalDataInstance instance, String reference,
            String stepType) {
        InstanceRoot instanceRoot = getExternalDataInstanceSource(reference, stepType);

        if (instanceRoot == null) {
            // Maintain backward compatibility with instance references that don't have a id in reference
            // should be removed once we move all external data instance connectors in existing apps to new
            // reference style jr://instance/<schema>/<id>
            if (isNonUniqueReference(reference)) {
                String referenceWithId = reference.concat("/").concat(instance.getInstanceId());
                instanceRoot = getExternalDataInstanceSource(referenceWithId, stepType);

                // last attempt to find the instance
                // this is necessary for 'search-input' instances which do not follow the convention
                // of instance ref = base + instance Id:
                //    <instance id="search-input:results" ref="jr://instance/search-input/results />
                if (instanceRoot == null) {
                    instanceRoot = getExternalDataInstanceSourceById(instance.getInstanceId(), stepType);
                }
            }
        }


        if (instanceRoot == null) {
            instanceRoot = instance.getSource();
        }

        return instanceRoot == null ? ConcreteInstanceRoot.NULL : instanceRoot;
    }

    public static boolean isNonUniqueReference(String reference) {
        return reference.contentEquals(ExternalDataInstance.JR_REMOTE_REFERENCE) ||
                reference.contentEquals(ExternalDataInstance.JR_SELECTED_ENTITIES_REFERENCE) ||
                reference.contentEquals(ExternalDataInstance.JR_SEARCH_INPUT_REFERENCE);
    }

    protected InstanceRoot setupLedgerData(ExternalDataInstance instance) {
        if (stockbase == null) {
            stockbase = new LedgerInstanceTreeElement(instance.getBase(), mSandbox.getLedgerStorage());
        } else {
            //re-use the existing model if it exists.
            stockbase.rebase(instance.getBase());
        }
        return new ConcreteInstanceRoot(stockbase);
    }

    protected InstanceRoot setupCaseData(ExternalDataInstance instance) {
        if (casebase == null) {
            casebase = new CaseInstanceTreeElement(instance.getBase(), mSandbox.getCaseStorage());
        } else {
            //re-use the existing model if it exists.
            casebase.rebase(instance.getBase());
        }
        return new ConcreteInstanceRoot(casebase);
    }


    protected InstanceRoot setupFixtureData(ExternalDataInstance instance) {
        return new ConcreteInstanceRoot(loadFixtureRoot(instance, instance.getReference()));
    }

    protected TreeElement loadFixtureRoot(ExternalDataInstance instance,
            String reference) {
        String refId = VirtualInstances.getReferenceId(reference);
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

    protected InstanceRoot setupSessionData(ExternalDataInstance instance) {
        if (this.mPlatform == null) {
            throw new RuntimeException("Cannot generate session instance with undeclared platform!");
        }
        User u = mSandbox.getLoggedInUserUnsafe();
        TreeElement root =
                SessionInstanceBuilder.getSessionInstance(sessionWrapper.getFrame(), getDeviceId(),
                        getVersionString(), getCurrentDrift(), u.getUsername(), u.getUniqueId(),
                        u.getProperties(), getWindowWidth());
        root.setParent(instance.getBase());
        return new ConcreteInstanceRoot(root);
    }

    protected String getWindowWidth() {
        return sessionWrapper.getWindowWidth();
    }

    protected long getCurrentDrift() {
        return 0;
    }

    protected InstanceRoot getExternalDataInstanceSource(String reference, String stepType) {
        for (StackFrameStep step : sessionWrapper.getFrame().getSteps()) {
            if (step.getType().equals(stepType) && step.hasDataInstanceSource(reference)) {
                return step.getDataInstanceSource(reference);
            }
        }
        return null;
    }

    /**
     * Required for legacy instance support
     */
    protected InstanceRoot getExternalDataInstanceSourceById(String instanceId, String stepType) {
        for (StackFrameStep step : sessionWrapper.getFrame().getSteps()) {
            if (step.getType().equals(stepType)) {
                ExternalDataInstanceSource source = step.getDataInstanceSourceById(instanceId);
                if (source != null) {
                    return source;
                }
            }
        }
        return null;
    }

    protected String getDeviceId() {
        return "----";
    }

    public String getVersionString() {
        return "CommCare Version: " + mPlatform.getMajorVersion() + "." + mPlatform.getMinorVersion();
    }

    public static class FixtureInitializationException extends RuntimeException {

        public FixtureInitializationException(String lookupReference) {
            super(Localization.getWithDefault("lookup.table.missing.error",
                    new String[]{lookupReference},
                    "Unable to find lookup table: " + lookupReference));
        }
    }
}
