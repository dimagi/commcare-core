package org.javarosa.core.services.transport.payload;

import org.javarosa.core.util.MultiInputStream;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

/**
 * @author Clayton Sims
 * @date Dec 18, 2008
 */
public class MultiMessagePayload implements IDataPayload {
    /**
     * IDataPayload *
     */
    Vector payloads = new Vector();

    /**
     * Note: Only useful for serialization.
     */
    public MultiMessagePayload() {
        //ONLY FOR SERIALIZATION
    }

    /**
     * Adds a payload that should be sent as part of this
     * payload.
     *
     * @param payload A payload that will be transmitted
     *                after all previously added payloads.
     */
    public void addPayload(IDataPayload payload) {
        payloads.addElement(payload);
    }

    /**
     * @return A vector object containing each IDataPayload in this payload.
     */
    public Vector getPayloads() {
        return payloads;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.transport.IDataPayload#getPayloadStream()
     */
    @Override
    public InputStream getPayloadStream() throws IOException {
        MultiInputStream bigStream = new MultiInputStream();
        Enumeration en = payloads.elements();
        while (en.hasMoreElements()) {
            IDataPayload payload = (IDataPayload)en.nextElement();
            bigStream.addStream(payload.getPayloadStream());
        }
        bigStream.prepare();
        return bigStream;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        payloads = (Vector)ExtUtil.read(in, new ExtWrapListPoly(), pf);
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapListPoly(payloads));
    }

    @Override
    public Object accept(IDataPayloadVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public String getPayloadId() {
        return null;
    }

    @Override
    public int getPayloadType() {
        return IDataPayload.PAYLOAD_TYPE_MULTI;
    }

    @Override
    public long getLength() {
        int len = 0;
        Enumeration en = payloads.elements();
        while (en.hasMoreElements()) {
            IDataPayload payload = (IDataPayload)en.nextElement();
            len += payload.getLength();
        }
        return len;
    }
}

