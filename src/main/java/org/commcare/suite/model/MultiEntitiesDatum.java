package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MultiEntitiesDatum extends EntityDatum {

    private int maxSelectValue = -1;

    @SuppressWarnings("unused")
    public MultiEntitiesDatum() {
        // used in serialization
    }

    public MultiEntitiesDatum(String id, String nodeset, String shortDetail, String longDetail,
            String inlineDetail, String persistentDetail, String value, String autoselect,
            int maxSelectValue) {
        super(id, nodeset, shortDetail, longDetail, inlineDetail, persistentDetail, value,
                autoselect);
        this.maxSelectValue = maxSelectValue;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException,
            DeserializationException {
        super.readExternal(in, pf);
        maxSelectValue = ExtUtil.readInt(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeNumeric(out, maxSelectValue);
    }

    public int getMaxSelectValue() {
        return maxSelectValue;
    }
}
