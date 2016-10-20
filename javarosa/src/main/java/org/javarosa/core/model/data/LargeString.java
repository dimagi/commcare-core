package org.javarosa.core.model.data;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExternalizableWrapper;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Stores string data that is larger than 65535 bytes long.
 *
 * Useful because our traditional string serialization implementation has a
 * max size limit equivalent to the max value of an unsigned short
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class LargeString extends ExternalizableWrapper {

    @SuppressWarnings("unused")
    public LargeString() {
    }

    public LargeString(String val) {
        this.val = val;
    }

    @Override
    public ExternalizableWrapper clone(Object val) {
        return new LargeString((String)val);
    }

    @Override
    public void metaReadExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {

    }

    @Override
    public void metaWriteExternal(DataOutputStream out) throws IOException {

    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        val = new String(ExtUtil.readBytes(in), "UTF-8");
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        byte[] stringAsBytes = ((String)val).getBytes("UTF-8");
        ExtUtil.writeBytes(out, stringAsBytes);
    }
}
