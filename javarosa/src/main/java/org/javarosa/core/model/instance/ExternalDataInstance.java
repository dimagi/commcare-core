package org.javarosa.core.model.instance;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author ctsims
 */
public class ExternalDataInstance extends DataInstance {
    private String reference;
    private boolean useCaseTemplate;

    private AbstractTreeElement root;
    private InstanceBase base;
    public final static String JR_REMOTE_REFERENCE = "jr://instance/remote";

    public ExternalDataInstance() {
    }

    public ExternalDataInstance(String reference, String instanceid) {
        super(instanceid);
        this.reference = reference;
    }

    /**
     * Copy constructor
     */
    public ExternalDataInstance(ExternalDataInstance instance) {
        super(instance.getInstanceId());

        this.reference = instance.getReference();
        this.base = instance.getBase();
        this.root = instance.getRoot();
        this.mCacheHost = instance.getCacheHost();
    }

    private ExternalDataInstance(String reference, String instanceId,
                                 TreeElement topLevel, boolean useCaseTemplate) {
        this(reference, instanceId);

        this.useCaseTemplate = useCaseTemplate;

        base = new InstanceBase(instanceId);
        topLevel.setInstanceName(instanceId);
        topLevel.setParent(base);
        this.root = topLevel;
        base.setChild(root);
    }

    public static ExternalDataInstance buildFromRemote(String instanceId,
                                                       TreeElement root,
                                                       boolean useCaseTemplate) {
        return new ExternalDataInstance(JR_REMOTE_REFERENCE, instanceId, root, useCaseTemplate);
    }

    public boolean useCaseTemplate() {
        return useCaseTemplate;
    }

    @Override
    public boolean isRuntimeEvaluated() {
        return true;
    }

    @Override
    public InstanceBase getBase() {
        return base;
    }

    @Override
    public AbstractTreeElement getRoot() {
        return root;
    }

    public String getReference() {
        return reference;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);

        reference = ExtUtil.readString(in);
        useCaseTemplate = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);

        ExtUtil.writeString(out, reference);
        ExtUtil.writeBool(out, useCaseTemplate);
    }

    @Override
    public DataInstance initialize(InstanceInitializationFactory initializer, String instanceId) {
        base = new InstanceBase(instanceId);
        root = initializer.generateRoot(this);
        base.setChild(root);
        return initializer.getSpecializedExternalDataInstance(this);
    }
}
