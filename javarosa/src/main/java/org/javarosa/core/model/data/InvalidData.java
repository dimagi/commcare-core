package org.javarosa.core.model.data;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Saumya on 6/27/2016.
 */
public class InvalidData implements IAnswerData {

    private String myErrorMessage;
    private IAnswerData savedValue;

    public InvalidData(String error, IAnswerData returnValue) {

        myErrorMessage = error;
        savedValue = returnValue;
    }

    @Override
    public void setValue(Object o) {
    }

    @Override
    public Object getValue() {
        return savedValue.getValue();
    }

    @Override
    public String getDisplayText() {
        return null;
    }

    @Override
    public IAnswerData clone() {
        return null;
    }

    @Override
    public UncastData uncast() {
        return new UncastData("Invalid Date was entered");
    }

    @Override
    public IAnswerData cast(UncastData data) throws IllegalArgumentException {
        return savedValue;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {}

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {}

    public String getErrorMessage() {
        return myErrorMessage;
    }
}
