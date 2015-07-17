package org.javarosa.core.model.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author Clayton Sims
 * @date May 19, 2009
 */
public class BooleanData implements IAnswerData {

    boolean data;

    /**
     * NOTE: ONLY FOR SERIALIZATION
     */
    public BooleanData() {

    }

    public BooleanData(boolean data) {
        this.data = data;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.data.IAnswerData#clone()
     */
    public IAnswerData clone() {
        return new BooleanData(data);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.data.IAnswerData#getDisplayText()
     */
    public String getDisplayText() {
        if (data) {
            return "True";
        } else {
            return "False";
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.data.IAnswerData#getValue()
     */
    public Object getValue() {
        return new Boolean(data);
    }


    /* (non-Javadoc)
     * @see org.javarosa.core.model.data.IAnswerData#setValue(java.lang.Object)
     */
    public void setValue(Object o) {
        data = ((Boolean)o).booleanValue();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        data = in.readBoolean();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        out.writeBoolean(data);
    }

    public UncastData uncast() {
        return new UncastData(data ? "1" : "0");
    }

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
