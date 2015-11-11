package org.commcare.suite.model;

import org.commcare.session.SessionFrame;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

/**
 * @author ctsims
 */
public class StackFrameStep implements Externalizable {
    //Share the types with the commands
    private String elementType;
    private String id;
    private String value;
    private boolean valueIsXpath;
    private Hashtable<String, String> extras = new Hashtable<String, String>();

    /**
     * Serialization Only
     */
    public StackFrameStep() {

    }

    public StackFrameStep(String type, String id, String value) {
        this.elementType = type;
        this.id = id;
        this.value = value;
    }

    public StackFrameStep(String type, String id,
                          String value, boolean valueIsXpath) throws XPathSyntaxException {
        this.elementType = type;
        this.id = id;
        this.value = value;
        this.valueIsXpath = valueIsXpath;

        if (valueIsXpath) {
            //Run the parser to ensure that we will fail fast when _creating_ the step, not when
            //running it
            XPathParseTool.parseXPath(value);
        }
    }

    public String getType() {
        return elementType;
    }

    public String getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public boolean getValueIsXPath() {
        return valueIsXpath;
    }

    public void addExtra(String key, String value) {
        extras.put(key, value);
    }

    public String getExtra(String key) {
        return extras.get(key);
    }

    /**
     * Get a performed step to pass on to an actual frame
     *
     * @param ec Context to evaluate any parameters with
     * @return A step that can be added to a session frame
     */
    public StackFrameStep defineStep(EvaluationContext ec) {
        String finalValue;
        if (!valueIsXpath) {
            finalValue = value;
        } else {
            try {
                finalValue = XPathFuncExpr.toString(XPathParseTool.parseXPath(value).eval(ec));
            } catch (XPathSyntaxException e) {
                //This error makes no sense, since we parse the input for
                //validation when we create it!
                throw new XPathException(e.getMessage());
            }
        }

        //figure out how to structure the step
        if (elementType.equals(SessionFrame.STATE_DATUM_VAL)) {
            return new StackFrameStep(SessionFrame.STATE_DATUM_VAL, id, finalValue);
        } else if (elementType.equals(SessionFrame.STATE_COMMAND_ID)) {
            return new StackFrameStep(SessionFrame.STATE_COMMAND_ID, finalValue, null);
        } else if (elementType.equals(SessionFrame.STATE_FORM_XMLNS)) {
            throw new RuntimeException("Form Definitions in Steps are not yet supported!");
        } else {
            throw new RuntimeException("Invalid step [" + elementType + "] declared when constructing a new frame step");
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        this.elementType = ExtUtil.readString(in);
        this.id = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        this.value = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        this.valueIsXpath = ExtUtil.readBool(in);
        this.extras = (Hashtable<String, String>)ExtUtil.read(in, new ExtWrapMap(String.class, String.class));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, elementType);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(id));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(value));
        ExtUtil.writeBool(out, valueIsXpath);
        ExtUtil.write(out, new ExtWrapMap(extras));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StackFrameStep)) {
            return false;
        }

        StackFrameStep that = (StackFrameStep)o;

        return ((propertiesEqual(this.getType(), that.getType())) &&
                (propertiesEqual(this.getId(), that.getId())) &&
                (propertiesEqual(this.getValue(), that.getValue())) &&
                (this.getValueIsXPath() == that.getValueIsXPath()));
    }

    @Override
    public int hashCode() {
        final int valueIsXPathHash = getValueIsXPath() ? 1231 : 1237;
        return (getType().hashCode() ^ getId().hashCode() ^
                getValue().hashCode() ^ valueIsXPathHash);
    }

    private boolean propertiesEqual(String a, String b) {
        if (a == null) {
            return b == null;
        } else {
            return (a.equals(b));
        }
    }

    @Override
    public String toString() {
        if (value ==  null) {
            return "(" + elementType + " " + id + ")";
        } else {
            return "(" + elementType + " " + id + " : " + value + ")";
        }
    }
}
