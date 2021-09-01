package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * A stack operation descriptor, containing all of the relevant details
 * about which operation to perform and any associated metadata
 *
 * @author ctsims
 */
public class StackOperation implements Externalizable {

    public static final int OPERATION_CREATE = 0;
    public static final int OPERATION_PUSH = 1;
    public static final int OPERATION_CLEAR = 2;

    private int opType;
    private String ifCondition;
    private Vector<StackFrameStep> elements;

    /**
     * Deserialization Only!
     */
    public StackOperation() {

    }

    // Copy Constructor
    public StackOperation(StackOperation oldStackOp) {
        this.opType = oldStackOp.opType;
        this.ifCondition = oldStackOp.ifCondition;
        this.elements = new Vector<>(oldStackOp.elements.size());
        for (StackFrameStep element : oldStackOp.elements) {
            elements.add(new StackFrameStep(element));
        }
    }

    public static StackOperation buildCreateFrame(String ifCondition,
                                                  Vector<StackFrameStep> elements) throws XPathSyntaxException {
        return new StackOperation(OPERATION_CREATE, ifCondition, elements);
    }

    public static StackOperation buildPushFrame(String ifCondition,
                                                Vector<StackFrameStep> elements) throws XPathSyntaxException {
        return new StackOperation(OPERATION_PUSH, ifCondition, elements);
    }

    public static StackOperation buildClearFrame(String ifCondition) throws XPathSyntaxException {
        return new StackOperation(OPERATION_CLEAR, ifCondition, new Vector<>());
    }

    private StackOperation(int opType, String ifCondition,
                           Vector<StackFrameStep> elements) throws XPathSyntaxException {
        this.opType = opType;
        this.ifCondition = ifCondition;
        if (ifCondition != null) {
            XPathParseTool.parseXPath(ifCondition);
        }
        this.elements = elements;
    }

    public int getOp() {
        return opType;
    }

    public boolean isOperationTriggered(EvaluationContext ec) {
        if (ifCondition != null) {
            try {
                return FunctionUtils.toBoolean(XPathParseTool.parseXPath(ifCondition).eval(ec));
            } catch (XPathSyntaxException e) {
                //This error makes no sense, since we parse the input for
                //validation when we create it!
                throw new XPathException(e.getMessage());
            }
        } else {
            return true;
        }
    }

    /**
     * Get the actual steps to be added (un-processed) to a frame.
     *
     * @return The definitions for the steps that should be included in this operation
     * @throws IllegalStateException if this operation do not support stack frame steps
     */
    public Vector<StackFrameStep> getStackFrameSteps() {
        if (opType == OPERATION_CLEAR) {
            throw new IllegalStateException("Clear Operations do not define frame steps");
        }
        return elements;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        opType = ExtUtil.readInt(in);
        ifCondition = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        elements = (Vector<StackFrameStep>)ExtUtil.read(in, new ExtWrapList(StackFrameStep.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, opType);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(ifCondition));
        ExtUtil.write(out, new ExtWrapList(elements));
    }

    @Override
    public String toString() {
        return "StackOperation " + elements;
    }
}
