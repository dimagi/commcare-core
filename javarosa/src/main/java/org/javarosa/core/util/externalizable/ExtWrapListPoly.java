package org.javarosa.core.util.externalizable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;


//list of objects of multiple types
//if elements are compound types (i.e., need wrappers), they must be pre-wrapped before invoking this wrapper, because... come on now.
public class ExtWrapListPoly extends ExternalizableWrapper {
    /* serializaiton */

    public ExtWrapListPoly(Vector val) {
        if (val == null) {
            throw new NullPointerException();
        }

        this.val = val;
    }

    /* deserialization */

    public ExtWrapListPoly() {

    }

    @Override
    public ExternalizableWrapper clone(Object val) {
        return new ExtWrapListPoly((Vector)val);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        long size = ExtUtil.readNumeric(in);
        Vector<Object> v = new Vector<>((int)size);
        for (int i = 0; i < size; i++) {
            v.addElement(ExtUtil.read(in, new ExtWrapTagged(), pf));
        }

        val = v;
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        Vector v = (Vector)val;

        ExtUtil.writeNumeric(out, v.size());
        for (int i = 0; i < v.size(); i++) {
            ExtUtil.write(out, new ExtWrapTagged(v.elementAt(i)));
        }
    }

    @Override
    public void metaReadExternal(DataInputStream in, PrototypeFactory pf) {
        //do nothing
    }

    @Override
    public void metaWriteExternal(DataOutputStream out) {
        //do nothing
    }
}
