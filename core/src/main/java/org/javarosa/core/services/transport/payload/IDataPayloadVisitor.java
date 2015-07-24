package org.javarosa.core.services.transport.payload;

/**
 * @author Clayton Sims
 * @date Dec 18, 2008
 */
public interface IDataPayloadVisitor<T> {
    public T visit(ByteArrayPayload payload);

    public T visit(MultiMessagePayload payload);

    public T visit(DataPointerPayload payload);
}
