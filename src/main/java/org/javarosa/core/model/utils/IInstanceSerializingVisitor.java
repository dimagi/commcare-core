package org.javarosa.core.model.utils;

import org.javarosa.core.model.IAnswerDataSerializer;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.transport.payload.IDataPayload;
import org.javarosa.model.xform.SerializationContext;
import org.javarosa.model.xform.XPathReference;

import java.io.IOException;

/**
 * An IInstanceSerializingVisitor serializes a DataModel
 *
 * @author Clayton Sims
 */
public interface IInstanceSerializingVisitor extends IInstanceVisitor {

    byte[] serializeInstance(FormInstance model, XPathReference ref) throws IOException;

    byte[] serializeInstance(FormInstance model) throws IOException;

    IDataPayload createSerializedPayload(FormInstance model, XPathReference ref) throws IOException;

    IDataPayload createSerializedPayload(FormInstance model) throws IOException;

    void setAnswerDataSerializer(IAnswerDataSerializer ads);

    IInstanceSerializingVisitor newInstance();
}
