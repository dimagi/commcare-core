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

    private AbstractTreeElement root;
    private InstanceBase base;

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
                                 AbstractTreeElement root) {
        this(reference, instanceId);

        base = new InstanceBase(instanceId);
        this.root = root;
        base.setChild(root);
    }

    public static ExternalDataInstance buildFromRemote(String instanceId,
                                               AbstractTreeElement root) {
        return new ExternalDataInstance("jr://instance/remote", instanceId, root);
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
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeString(out, reference);
    }

    @Override
    public DataInstance initialize(InstanceInitializationFactory initializer, String instanceId) {
        base = new InstanceBase(instanceId);
        root = initializer.generateRoot(this);
        base.setChild(root);
        return initializer.getSpecializedExternalDataInstance(this);
    }

}
