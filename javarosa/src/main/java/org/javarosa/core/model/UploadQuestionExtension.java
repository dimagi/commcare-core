package org.javarosa.core.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Represents any additional information included in an "upload" question type via extra
 * attributes that are parsed by UploadQuestionExtensionParser
 *
 * @author amstone
 */

public class UploadQuestionExtension implements QuestionDataExtension {

    private int maxDimen;

    @SuppressWarnings("unused")
    public UploadQuestionExtension() {
        // for deserialization
    }

    public UploadQuestionExtension(int maxDimen) {
        this.maxDimen = maxDimen;
    }

    public int getMaxDimen() {
        return this.maxDimen;
    }

    @Override
    public void readExternal(DataInputStream dis, PrototypeFactory pf)
            throws IOException, DeserializationException {
        this.maxDimen = ExtUtil.readInt(dis);
    }

    @Override
    public void writeExternal(DataOutputStream dos) throws IOException {
        ExtUtil.writeNumeric(dos, maxDimen);
    }
}