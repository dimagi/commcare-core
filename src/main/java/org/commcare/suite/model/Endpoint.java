package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

public class Endpoint implements Externalizable {

    String id;
    boolean respectRelevancy;
    Vector<EndpointArgument> arguments;
    Vector<StackOperation> stackOperations;

    // for serialization
    public Endpoint() {
    }

    public Endpoint(String id, Vector<EndpointArgument> arguments, Vector<StackOperation> stackOperations, boolean respectRelevancy) {
        this.id = id;
        this.arguments = arguments;
        this.stackOperations = stackOperations;
        this.respectRelevancy = respectRelevancy;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        id = ExtUtil.readString(in);
        arguments = (Vector<EndpointArgument>)ExtUtil.read(in, new ExtWrapList(EndpointArgument.class), pf);
        stackOperations = (Vector<StackOperation>)ExtUtil.read(in, new ExtWrapList(StackOperation.class), pf);
        respectRelevancy = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, id);
        ExtUtil.write(out, new ExtWrapList(arguments));
        ExtUtil.write(out, new ExtWrapList(stackOperations));
        ExtUtil.writeBool(out, respectRelevancy);
    }

    public String getId() {
        return id;
    }

    public Vector<EndpointArgument> getArguments() {
        return arguments;
    }

    public Vector<StackOperation> getStackOperations() {
        return stackOperations;
    }

    public boolean isRespectRelevancy() {
        return respectRelevancy;
    }

    // Utility Functions
    public static void populateEndpointArgumentsToEvaluationContext(Endpoint endpoint, ArrayList<String> args, EvaluationContext evaluationContext) {
        Vector<EndpointArgument> endpointArguments = endpoint.getArguments();

        if (endpointArguments.size() > args.size()) {
            Vector<String> missingArguments = new Vector<>();
            for (int i = args.size(); i < endpointArguments.size(); i++) {
                missingArguments.add(endpointArguments.get(i).getId());
            }
            throw new InvalidEndpointArgumentsException(missingArguments, null);
        }

        for (int i = 0; i < endpointArguments.size(); i++) {
            String argumentName = endpointArguments.elementAt(i).getId();
            evaluationContext.setVariable(argumentName, args.get(i));
        }
    }

    public static void populateEndpointArgumentsToEvaluationContext(Endpoint endpoint, HashMap<String, String> args, EvaluationContext evaluationContext) {
        Vector<EndpointArgument> endpointArguments = endpoint.getArguments();
        Set<String> argumentIds = args.keySet();
        Vector<String> missingArguments = new Vector<>();
        for (EndpointArgument endpointArgument : endpointArguments) {
            if(!argumentIds.contains(endpointArgument.getId())){
                missingArguments.add(endpointArgument.getId());
            }
        }

//        Vector<String> unexpectedArguments = new Vector<>();
//        for (String argumentId : argumentIds) {
//            if(!isValidArgumentId(endpointArguments, argumentId)){
//                unexpectedArguments.add(argumentId);
//            }
//        }

//        if (missingArguments.size() > 0 || unexpectedArguments.size() > 0) {
//            throw new InvalidEndpointArgumentsException(missingArguments, unexpectedArguments);
//        }

        for (int i = 0; i < endpointArguments.size(); i++) {
            EndpointArgument argument = endpointArguments.elementAt(i);
            String argumentName = argument.getId();
            if (args.containsKey(argumentName)) {
                evaluationContext.setVariable(argumentName, args.get(argumentName));
            }
            if (argument.isInstanceArgument()) {
                argumentName = "instance_id_" + argumentName;
                if (args.containsKey(argumentName)) {
                    evaluationContext.setVariable(argumentName, args.get(argumentName));
                }
            }
        }
    }

    private static boolean isValidArgumentId(Vector<EndpointArgument> endpointArguments, String argumentId) {
        for (EndpointArgument endpointArgument : endpointArguments) {
            if (endpointArgument.getId().contentEquals(argumentId)) {
                return true;
            }
        }
        return false;
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
