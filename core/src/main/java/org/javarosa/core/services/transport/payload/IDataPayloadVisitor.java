package org.javarosa.core.services.transport.payload;

/**
 * @author Clayton Sims
 * @date Dec 18, 2008
 */
public interface IDataPayloadVisitor<T> {
    T visit(ByteArrayPayload payload);

    T visit(MultiMessagePayload payload);

    T visit(DataPointerPayload payload);
}
