package org.javarosa.core.model.data;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A response to a question requesting an Long Numeric Value
 *
 * @author Clayton Sims
 */
public class LongData implements IAnswerData {
    private long n;

    /**
     * Empty Constructor, necessary for dynamic construction during deserialization.
     * Shouldn't be used otherwise.
     */
    public LongData() {

    }

    public LongData(long n) {
        this.n = n;
    }

    public LongData(Long n) {
        setValue(n);
    }

    @Override
    public IAnswerData clone() {
        return new LongData(n);
    }

    @Override
    public String getDisplayText() {
        return String.valueOf(n);
    }

    @Override
    public Object getValue() {
        return n;
    }

    @Override
    public void setValue(Object o) {
        if (o == null) {
            throw new NullPointerException("Attempt to set an IAnswerData class to null.");
        }
        n = (Long)o;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        n = ExtUtil.readNumeric(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, n);
    }

    @Override
    public UncastData uncast() {
        return new UncastData(new Long(n).toString());
    }

    @Override
    public LongData cast(UncastData data) throws IllegalArgumentException {
        try {
            return new LongData(Long.parseLong(data.value));
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Invalid cast of data [" + data.value + "] to type Long");
        }
    }
}
