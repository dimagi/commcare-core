package org.commcare.suite.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.commcare.session.SessionFrame;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMultiMap;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * @author ctsims
 */
public class StackFrameStep implements Externalizable {
    //Share the types with the commands
    private String elementType;
    private String id;
    private String value;
    private boolean valueIsXpath;
    private Multimap<String, Object> extras = ArrayListMultimap.create();

    /**
     * XML instance collected during session navigation that is made available
     * in the session's evaluation context. For instance, useful to store
     * results of a query command during case search and claim workflow
     */
    private ExternalDataInstanceSource xmlInstanceSource;

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
        extras.putAll(oldStackFrameStep.getExtras());

        if (oldStackFrameStep.hasXmlInstance()) {
            this.xmlInstanceSource = new ExternalDataInstanceSource(oldStackFrameStep.xmlInstanceSource);
        }
    }

    public StackFrameStep(String type, String id, String value) {
        this.elementType = type;
        this.id = id;
        this.value = value;
    }

    public StackFrameStep(String type, String id, String value,
                          ExternalDataInstanceSource xmlInstanceSource) {
        this(type, id, value);

        this.xmlInstanceSource = xmlInstanceSource;
    }

    public StackFrameStep(String type, String id,
                          String value, boolean valueIsXpath) throws XPathSyntaxException {
        this(type, id, value);

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

    public boolean hasXmlInstance() {
        return xmlInstanceSource != null;
    }

    public ExternalDataInstanceSource getXmlInstanceSource() {
        return xmlInstanceSource;
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
        Collection<Object> values = extras.get(key);
        if (values.size() > 1) {
            throw new RuntimeException(String.format("Multiple extras found with key %s", key));
        }
        try {
            return values.iterator().next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public Multimap<String, Object> getExtras() {
        return extras;
    }

    /**
     * Get a performed step to pass on to an actual frame
     *
     * @param ec          Context to evaluate any parameters with
     * @param neededDatum The current datum needed by the session, used by
     *                    'mark' to know what datum to set in a 'rewind'
     * @return A step that can be added to a session frame
     */
    public StackFrameStep defineStep(EvaluationContext ec, SessionDatum neededDatum) {
        switch (elementType) {
            case SessionFrame.STATE_DATUM_VAL:
                return new StackFrameStep(SessionFrame.STATE_DATUM_VAL, id, evaluateValue(ec));
            case SessionFrame.STATE_COMMAND_ID:
                return new StackFrameStep(SessionFrame.STATE_COMMAND_ID, evaluateValue(ec), null);
            case SessionFrame.STATE_UNKNOWN:
                return new StackFrameStep(SessionFrame.STATE_UNKNOWN, id, evaluateValue(ec));
            case SessionFrame.STATE_REWIND:
                return new StackFrameStep(SessionFrame.STATE_REWIND, null, evaluateValue(ec));
            case SessionFrame.STATE_MARK:
                if (neededDatum == null) {
                    throw new RuntimeException("Can't add a mark in a place where there is no needed datum");
                }
                return new StackFrameStep(SessionFrame.STATE_MARK, neededDatum.getDataId(), null);
            case SessionFrame.STATE_FORM_XMLNS:
                throw new RuntimeException("Form Definitions in Steps are not yet supported!");
            case SessionFrame.STATE_QUERY_REQUEST:
            case SessionFrame.STATE_SMART_LINK:
                StackFrameStep defined = new StackFrameStep(elementType, id, evaluateValue(ec));
                extras.forEach((key, value) -> {
                    XPathExpression expr = (XPathExpression) value;
                    defined.addExtra(key, FunctionUtils.toString(expr.eval(ec)));
                });
                return defined;
            default:
                throw new RuntimeException("Invalid step [" + elementType + "] declared when constructing a new frame step");
        }
    }

    public String evaluateValue(EvaluationContext ec) {
        if (!valueIsXpath) {
            return value;
        } else {
            try {
                return FunctionUtils.toString(XPathParseTool.parseXPath(value).eval(ec));
            } catch (XPathSyntaxException e) {
                //This error makes no sense, since we parse the input for
                //validation when we create it!
                throw new XPathException(e.getMessage());
            }
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        this.elementType = ExtUtil.readString(in);
        this.id = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        this.value = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        this.valueIsXpath = ExtUtil.readBool(in);
        this.extras = (Multimap<String, Object>)ExtUtil.read(in, new ExtWrapMultiMap(String.class), pf);
        this.xmlInstanceSource = (ExternalDataInstanceSource)ExtUtil.read(in, new ExtWrapNullable(ExternalDataInstanceSource.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, elementType);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(id));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(value));
        ExtUtil.writeBool(out, valueIsXpath);
        ExtUtil.write(out, new ExtWrapMultiMap(extras));
        ExtUtil.write(out, new ExtWrapNullable(xmlInstanceSource));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StackFrameStep)) {
            return false;
        }

        StackFrameStep that = (StackFrameStep)o;

        return ((propertiesEqual(this.elementType, that.elementType)) &&
                (propertiesEqual(this.id, that.id)) &&
                (propertiesEqual(this.value, that.value)) &&
                (this.valueIsXpath == that.valueIsXpath));
    }

    @Override
    public int hashCode() {
        final int valueIsXPathHash = valueIsXpath ? 1231 : 1237;
        return (elementType.hashCode() ^ id.hashCode() ^
                value.hashCode() ^ valueIsXPathHash);
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

    public void setType(String elementType) {
        this.elementType = elementType;
    }

    // Used by Formplayer
    @SuppressWarnings("unused")
    public String getElementType() {
        return elementType;
    }
}
