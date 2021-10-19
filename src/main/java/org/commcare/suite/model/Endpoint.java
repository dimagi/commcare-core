package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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


    // Utility Functions
    public static void populateEndpointArgumentsToEvaluationContext(Endpoint endpoint, ArrayList<String> args, EvaluationContext evaluationContext) {
        Vector<String> endpointArguments = endpoint.getArguments();

        if (endpointArguments.size() > args.size()) {
            Vector<String> missingArguments = new Vector<String>(endpointArguments.subList(args.size(), endpointArguments.size()));
            throw new InvalidEndpointArgumentsException(missingArguments, null);
        }

        for (int i = 0; i < endpointArguments.size(); i++) {
            String argumentName = endpointArguments.elementAt(i);
            evaluationContext.setVariable(argumentName, args.get(i));
        }
    }

    public static void populateEndpointArgumentsToEvaluationContext(Endpoint endpoint, HashMap<String, String> args, EvaluationContext evaluationContext) {
        Vector<String> endpointArguments = endpoint.getArguments();

        Vector<String> missingArguments = (Vector<String>)endpointArguments.clone();
        missingArguments.removeAll(args.keySet());

        Vector<String> unexpectedArguments = new Vector<String>(args.keySet());
        unexpectedArguments.removeAll(endpointArguments);

        if (missingArguments.size() > 0 || unexpectedArguments.size() > 0) {
            throw new InvalidEndpointArgumentsException(missingArguments, unexpectedArguments);
        }

        for (int i = 0; i < endpointArguments.size(); i++) {
            String argumentName = endpointArguments.elementAt(i);
            if (args.containsKey(argumentName)) {
                evaluationContext.setVariable(argumentName, args.get(argumentName));
            }
        }
    }

    public static class InvalidEndpointArgumentsException extends RuntimeException {
        private final Vector<String> missingArguments;
        private final Vector<String> unexpectedArguments;

        public InvalidEndpointArgumentsException(Vector<String> missingArguments, Vector<String> unexpectedArguments) {
            this.missingArguments = missingArguments;
            this.unexpectedArguments = unexpectedArguments;
        }

        public boolean hasMissingArguments() {
            return missingArguments != null && missingArguments.size() > 0;
        }

        public Vector<String> getMissingArguments() {
            return missingArguments;
        }

        public boolean hasUnexpectedArguments() {
            return unexpectedArguments != null && unexpectedArguments.size() > 0;
        }

        public Vector<String> getUnexpectedArguments() {
            return unexpectedArguments;
        }
    }
}
