package org.commcare.suite.model;

import org.commcare.session.SessionFrame;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMapPoly;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
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
    private Hashtable<String, Object> extras = new Hashtable<>();

    /**
     * XML instance collected during session navigation that is made available
     * in the session's evaulation context. For instance, useful to store
     * results of a query command during case search and claim workflow
     */
    private ExternalDataInstance xmlInstance;

    /**
     * Serialization Only
     */
    public StackFrameStep() {

    }

    /**
     * Copy constructor
     */
    public StackFrameStep(StackFrameStep oldStackFrameStep) {
        this.elementType = oldStackFrameStep.elementType;
        this.id = oldStackFrameStep.id;
        this.value = oldStackFrameStep.value;
        this.valueIsXpath = oldStackFrameStep.valueIsXpath;
        for (Enumeration e = oldStackFrameStep.extras.keys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            // shallow copy of extra value
            extras.put(key, oldStackFrameStep.extras.get(key));
        }

        if (oldStackFrameStep.hasXmlInstance()) {
            this.xmlInstance = new ExternalDataInstance(oldStackFrameStep.xmlInstance);
        }
    }

    public StackFrameStep(String type, String id, String value) {
        this.elementType = type;
        this.id = id;
        this.value = value;
    }

    public StackFrameStep(String type, String id, String value,
                          ExternalDataInstance xmlInstance) {
        this(type, id, value);

        this.xmlInstance = xmlInstance;
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

    public boolean hasXmlInstance() {
        return xmlInstance != null;
    }

    public ExternalDataInstance getXmlInstance() {
        return xmlInstance;
    }

    /**
     * @param value Must extend Externalizable or be a basic data type (String, Vector, etc)
     */
    public void addExtra(String key, Object value) {
        if (value != null) {
            extras.put(key, value);
        }
    }

    public Object getExtra(String key) {
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
        switch (elementType) {
            case SessionFrame.STATE_DATUM_VAL:
                return new StackFrameStep(SessionFrame.STATE_DATUM_VAL, id, finalValue);
            case SessionFrame.STATE_COMMAND_ID:
                return new StackFrameStep(SessionFrame.STATE_COMMAND_ID, finalValue, null);
            case SessionFrame.STATE_RETURN:
                return new StackFrameStep(SessionFrame.STATE_RETURN, finalValue, null);
            case SessionFrame.STATE_FORM_XMLNS:
                throw new RuntimeException("Form Definitions in Steps are not yet supported!");
            default:
                throw new RuntimeException("Invalid step [" + elementType + "] declared when constructing a new frame step");
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        this.elementType = ExtUtil.readString(in);
        this.id = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        this.value = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        this.valueIsXpath = ExtUtil.readBool(in);
        this.extras = (Hashtable<String, Object>)ExtUtil.read(in, new ExtWrapMapPoly(String.class), pf);
        this.xmlInstance = (ExternalDataInstance)ExtUtil.read(in, new ExtWrapNullable(ExternalDataInstance.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, elementType);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(id));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(value));
        ExtUtil.writeBool(out, valueIsXpath);
        ExtUtil.write(out, new ExtWrapMapPoly(extras));
        ExtUtil.write(out, new ExtWrapNullable(xmlInstance));
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
