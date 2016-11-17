package org.javarosa.core.services.transport.payload;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A ByteArrayPayload is a simple payload consisting of a
 * byte array.
 *
 * @author Clayton Sims
 */
public class ByteArrayPayload implements IDataPayload {
    private byte[] payload;
    private String id;
    private int type;

    /**
     * Note: Only useful for serialization.
     */
    @SuppressWarnings("unused")
    public ByteArrayPayload() {
    }

    /**
     * @param payload The byte array for this payload.
     * @param id      An optional id identifying the payload
     * @param type    The type of data for this byte array
     */
    public ByteArrayPayload(byte[] payload, String id, int type) {
        this.payload = payload;
        this.id = id;
        this.type = type;
    }

    /**
     * @param payload The byte array for this payload.
     */
    public ByteArrayPayload(byte[] payload) {
        this.payload = payload;
        this.id = null;
        this.type = IDataPayload.PAYLOAD_TYPE_XML;
    }

    @Override
    public InputStream getPayloadStream() {

        return new ByteArrayInputStream(payload);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        int length = in.readInt();
        if (length > 0) {
            this.payload = new byte[length];
            in.read(this.payload);
        }
        id = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        out.writeInt(payload.length);
        if (payload.length > 0) {
            out.write(payload);
        }
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(id));
    }

    @Override
    public Object accept(IDataPayloadVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public String getPayloadId() {
        return id;
    }

    @Override
    public int getPayloadType() {
        return type;
    }

    @Override
    public long getLength() {
        return payload.length;
    }
}
