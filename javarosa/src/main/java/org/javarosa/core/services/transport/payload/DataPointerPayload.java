package org.javarosa.core.services.transport.payload;

import org.javarosa.core.data.IDataPointer;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A payload for a Pointer to some data.
 *
 * @author Clayton Sims
 * @date Dec 29, 2008
 */
public class DataPointerPayload implements IDataPayload {
    IDataPointer pointer;

    /**
     * Note: Only useful for serialization.
     */
    public DataPointerPayload() {
    }

    public DataPointerPayload(IDataPointer pointer) {
        this.pointer = pointer;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.transport.IDataPayload#accept(org.javarosa.core.services.transport.IDataPayloadVisitor)
     */
    @Override
    public <T> T accept(IDataPayloadVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.transport.IDataPayload#getLength()
     */
    @Override
    public long getLength() {
        //Unimplemented. This method will eventually leave the contract
        return pointer.getLength();
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.transport.IDataPayload#getPayloadId()
     */
    @Override
    public String getPayloadId() {
        return pointer.getDisplayText();
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.transport.IDataPayload#getPayloadStream()
     */
    @Override
    public InputStream getPayloadStream() throws IOException {
        return pointer.getDataStream();
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.transport.IDataPayload#getPayloadType()
     */
    @Override
    public int getPayloadType() {
        String display = pointer.getDisplayText();
        if (display == null || display.lastIndexOf('.') == -1) {
            //uhhhh....?
            return IDataPayload.PAYLOAD_TYPE_TEXT;
        }

        String ext = display.substring(display.lastIndexOf('.') + 1);

        if (ext.equals("jpg") || ext.equals("jpeg")) {
            return IDataPayload.PAYLOAD_TYPE_JPG;
        }

        return IDataPayload.PAYLOAD_TYPE_JPG;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        pointer = (IDataPointer)ExtUtil.read(in, new ExtWrapTagged());
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(pointer));
    }
}
