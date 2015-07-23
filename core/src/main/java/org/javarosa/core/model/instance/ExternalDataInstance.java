/**
 *
 */
package org.javarosa.core.model.instance;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 */
public class ExternalDataInstance extends DataInstance {

    String reference;
    int recordid = -1;

    AbstractTreeElement root;
    InstanceBase base;

    public ExternalDataInstance() {

    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.model.instance.DataInstance#isRuntimeEvaluated()
     */
    public boolean isRuntimeEvaluated() {
        return true;
    }


    public InstanceBase getBase() {
        return base;
    }

    public ExternalDataInstance(String reference, String instanceid) {
        super(instanceid);
        this.reference = reference;
    }

    public AbstractTreeElement getRoot() {
        return root;
    }

    public String getReference() {
        return reference;
    }

    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);
        reference = ExtUtil.readString(in);
    }

    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeString(out, reference);
    }

    public void initialize(InstanceInitializationFactory initializer, String instanceId) {
        base = new InstanceBase(instanceId);
        root = initializer.generateRoot(this);
        base.setChild(root);
    }
}
