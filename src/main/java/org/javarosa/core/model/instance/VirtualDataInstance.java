package org.javarosa.core.model.instance;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Data instance that is created during execution based on user input of other live data
 */
public class VirtualDataInstance extends DataInstance {
    private TreeElement root = new TreeElement();
    private InstanceBase base;

    public VirtualDataInstance() {
    }

    public VirtualDataInstance(String instanceid) {
        super(instanceid);
    }

    /**
     * Copy constructor
     */
    public VirtualDataInstance(VirtualDataInstance instance) {
        super(instance.getInstanceId());
        this.base = instance.getBase();
        //Copy constructor avoids check.
        this.root = instance.root;
        this.mCacheHost = instance.getCacheHost();
    }

    public VirtualDataInstance(String instanceId, TreeElement topLevel) {
        this(instanceId);
        base = new InstanceBase(instanceId);
        topLevel.setInstanceName(instanceId);
        topLevel.setParent(base);
        this.root = topLevel;
        base.setChild(root);
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

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);
        root = (TreeElement)ExtUtil.read(in, TreeElement.class, pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.write(out, getRoot());
    }

    @Override
    public DataInstance initialize(InstanceInitializationFactory initializer, String instanceId) {
        this.instanceid = instanceId;
        root.setInstanceName(instanceId);
        return this;
    }
}
