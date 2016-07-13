package org.javarosa.core.model.data;

import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * A response to a question requesting an Integer Value
 *
 * @author Clayton Sims
 */
public class IntegerData implements IAnswerData {
    private int n;

    /**
     * Empty Constructor, necessary for dynamic construction during deserialization.
     * Shouldn't be used otherwise.
     */
    public IntegerData() {

    }

    public IntegerData(int n) {
        this.n = n;
    }

    public IntegerData(Integer n) {
        setValue(n);
    }

    @Override
    public IAnswerData clone() {
        return new IntegerData(n);
    }

    @Override
    public String getDisplayText() {
        return String.valueOf(n);
    }

    @Override
    public Object getValue() {
        return DataUtil.integer(n);
    }

    @Override
    public void setValue(Object o) {
        if (o == null) {
            throw new NullPointerException("Attempt to set an IAnswerData class to null.");
        }
        n = (Integer)o;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        n = ExtUtil.readInt(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, n);
    }

    @Override
    public UncastData uncast() {
        return new UncastData(DataUtil.integer(n).toString());
    }

    @Override
    public IntegerData cast(UncastData data) throws IllegalArgumentException {
        try {
            return new IntegerData(Integer.parseInt(data.value));
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Invalid cast of data [" + data.value + "] to type Decimal");
        }
    }
}
