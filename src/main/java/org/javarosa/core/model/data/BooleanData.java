package org.javarosa.core.model.data;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Clayton Sims
 * @date May 19, 2009
 */
public class BooleanData implements IAnswerData {

    private boolean data;

    /**
     * NOTE: ONLY FOR SERIALIZATION
     */
    public BooleanData() {

    }

    public BooleanData(boolean data) {
        this.data = data;
    }

    @Override
    public IAnswerData clone() {
        return new BooleanData(data);
    }

    @Override
    public String getDisplayText() {
        if (data) {
            return "True";
        } else {
            return "False";
        }
    }

    @Override
    public Object getValue() {
        return data;
    }


    @Override
    public void setValue(Object o) {
        data = (Boolean)o;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        data = in.readBoolean();
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        out.writeBoolean(data);
    }

    @Override
    public UncastData uncast() {
        return new UncastData(data ? "1" : "0");
    }

    @Override
    public BooleanData cast(UncastData data) throws IllegalArgumentException {
        if ("1".equals(data)) {
            return new BooleanData(true);
        }

        if ("0".equals(data)) {
            return new BooleanData(false);
        }

        throw new IllegalArgumentException("Invalid cast of data [" + data.value + "] to type Boolean");
    }
}
