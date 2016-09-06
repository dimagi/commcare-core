package org.commcare.core.process;

import org.commcare.cases.instance.CaseDataInstance;
import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.ledger.instance.LedgerInstanceTreeElement;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.sandbox.SandboxUtils;
import org.commcare.session.SessionInstanceBuilder;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.User;
import org.commcare.session.CommCareSession;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.locale.Localization;

/**
 *  Initializes a CommCare DataInstance against a UserDataInterface and (sometimes) optional
 *  CommCareSession/Platform
 *
 * @author ctsims
 * @author wspride
 */
public class CommCareInstanceInitializer extends InstanceInitializationFactory {

    protected final CommCareSession session;
    protected CaseInstanceTreeElement casebase;
    protected LedgerInstanceTreeElement stockbase;
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

    public CommCareInstanceInitializer copyWithNewSession(CommCareSession session) {
        return new CommCareInstanceInitializer(session, mSandbox, mPlatform);
    }

    @Override
    public ExternalDataInstance getSpecializedExternalDataInstance(ExternalDataInstance instance) {
        if (CaseInstanceTreeElement.MODEL_NAME.equals(instance.getInstanceId())) {
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
            casebase = new CaseInstanceTreeElement(instance.getBase(), mSandbox.getCaseStorage(), false);
        } else {
            //re-use the existing model if it exists.
            casebase.rebase(instance.getBase());
        }
        return casebase;
    }


    protected AbstractTreeElement setupFixtureData(ExternalDataInstance instance) {
        String ref = instance.getReference();
        //TODO: This is all just copied from J2ME code. that's pretty silly. unify that.
        String userId = "";
        User u = mSandbox.getLoggedInUser();

        if (u != null) {
            userId = u.getUniqueId();
        }

        String refId = ref.substring(ref.lastIndexOf('/') + 1, ref.length());
        try {
            FormInstance fixture = SandboxUtils.loadFixture(mSandbox, refId, userId);

            if (fixture == null) {
                throw new FixtureInitializationException(ref);
            }

            TreeElement root = fixture.getRoot();
            root.setParent(instance.getBase());
            return root;
        } catch (IllegalStateException ise) {
            throw new FixtureInitializationException(ref);
        }
    }

    protected AbstractTreeElement setupSessionData(ExternalDataInstance instance) {
        if (this.mPlatform == null) {
            throw new RuntimeException("Cannot generate session instance with undeclared platform!");
        }
        User u = mSandbox.getLoggedInUser();
        TreeElement root =
                SessionInstanceBuilder.getSessionInstance(session.getFrame(), getDeviceId(),
                        getVersionString(), u.getUsername(), u.getUniqueId(),
                        u.getProperties()).getRoot();
        root.setParent(instance.getBase());
        return root;
    }

    protected String getDeviceId(){
        return "----";
    }

    protected String getVersionString(){
        return "CommCare Version: " + mPlatform.getMajorVersion() + "." + mPlatform.getMinorVersion();
    }

    protected AbstractTreeElement setupMigrationData(ExternalDataInstance instance) {
        return null;
    }

    public static class FixtureInitializationException extends RuntimeException {

        public FixtureInitializationException(String lookupReference) {
            super(Localization.getWithDefault("lookup.table.missing.error",
                    new String[] {lookupReference},
                    "Unable to find lookup table: " + lookupReference));
        }
    }
}
