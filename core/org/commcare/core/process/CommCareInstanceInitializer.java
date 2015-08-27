package org.commcare.core.process;

import org.commcare.core.interfaces.AbstractUserSandbox;
import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.ledger.instance.LedgerInstanceTreeElement;
import org.commcare.core.sandbox.SandboxUtils;
import org.javarosa.core.model.User;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareSession;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;

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
    protected final AbstractUserSandbox mSandbox;
    protected final CommCarePlatform mPlatform;


    public CommCareInstanceInitializer(AbstractUserSandbox sandbox) {
        this(null, sandbox, null);
    }

    public CommCareInstanceInitializer(AbstractUserSandbox sandbox, CommCarePlatform platform) {
        this(null, sandbox, platform);
    }

    public CommCareInstanceInitializer(AbstractUserSandbox sandbox, CommCareSession session) {
        this(session, sandbox, null);
    }

    public CommCareInstanceInitializer(CommCareSession session, AbstractUserSandbox sandbox, CommCarePlatform platform) {
        this.session = session;
        this.mSandbox = sandbox;
        this.mPlatform = platform;
    }

    public AbstractTreeElement generateRoot(ExternalDataInstance instance) {
        String ref = instance.getReference();
        if (ref.indexOf(LedgerInstanceTreeElement.MODEL_NAME) != -1) {
            if (stockbase == null) {
                stockbase = new LedgerInstanceTreeElement(instance.getBase(), mSandbox.getLedgerStorage());
            } else {
                //re-use the existing model if it exists.
                stockbase.rebase(instance.getBase());
            }
            return stockbase;
        } else if (ref.indexOf(CaseInstanceTreeElement.MODEL_NAME) != -1) {
            if (casebase == null) {
                casebase = new CaseInstanceTreeElement(instance.getBase(), mSandbox.getCaseStorage(), false);
            } else {
                //re-use the existing model if it exists.
                casebase.rebase(instance.getBase());
            }
            return casebase;
        } else if (instance.getReference().indexOf("fixture") != -1) {
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
                    throw new RuntimeException("Could not find an appropriate fixture for src: " + ref);
                }

                TreeElement root = fixture.getRoot();
                root.setParent(instance.getBase());
                return root;

            } catch (IllegalStateException ise) {
                throw new RuntimeException("Could not load fixture for src: " + ref);
            }
        }
        if (instance.getReference().indexOf("session") != -1) {
            if(this.mPlatform == null) {
                throw new RuntimeException("Cannot generate session instance with undeclared platform!");
            }
            User u = mSandbox.getLoggedInUser();
            TreeElement root = session.getSessionInstance(getDeviceId(), getVersionString(), u.getUsername(), u.getUniqueId(), u.getProperties()).getRoot();
            root.setParent(instance.getBase());
            return root;
        }
        return null;
    }

    protected String getDeviceId(){
        return "----";
    }

    protected String getVersionString(){
        return "CommCare CLI: " + mPlatform.getMajorVersion() + "." + mPlatform.getMinorVersion();
    }
}
