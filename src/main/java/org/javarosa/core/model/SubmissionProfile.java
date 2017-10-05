/**
 *
 */
package org.javarosa.core.model;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

/**
 * A Submission Profile is a class which is responsible for
 * holding and processing the details of how a <submission/>
 * should be handled.
 *
 * @author ctsims
 */
public class SubmissionProfile implements Externalizable {

    TreeReference targetref;
    TreeReference ref;

    String resource;

    public SubmissionProfile() {

    }

    public SubmissionProfile(String resource, TreeReference targetRef, TreeReference ref) {
        this.ref = ref;
        this.targetref = targetRef;
        this.resource = resource;
    }

    public TreeReference getTargetRef() {
        return targetref;
    }

    public TreeReference getRef() {
        return ref;
    }

    public String getResource() {
        return resource;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        this.resource = ExtUtil.readString(in);
        this.targetref = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
        this.ref = (TreeReference)ExtUtil.read(in, new ExtWrapNullable(TreeReference.class), pf);

    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, this.resource);
        ExtUtil.write(out, this.targetref);
        ExtUtil.write(out, new ExtWrapNullable(this.ref));
    }


}
