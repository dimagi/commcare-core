package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

public class Endpoint implements Externalizable {

    String id;
    Vector<String> arguments;
    Vector<StackOperation> stackOperations;

    // for serialization
    public Endpoint() {
    }

    public Endpoint(String id, Vector<String> arguments, Vector<StackOperation> stackOperations) {
        this.id = id;
        this.arguments = arguments;
        this.stackOperations = stackOperations;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        id = ExtUtil.readString(in);
        arguments = (Vector<String>)ExtUtil.read(in, new ExtWrapList(String.class), pf);
        stackOperations = (Vector<StackOperation>)ExtUtil.read(in, new ExtWrapList(StackOperation.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, id);
        ExtUtil.write(out, new ExtWrapList(arguments));
        ExtUtil.write(out, new ExtWrapList(stackOperations));
    }

    public String getId() {
        return id;
    }

    public Vector<String> getArguments() {
        return arguments;
    }

    public Vector<StackOperation> getStackOperations() {
        return stackOperations;
    }
}
