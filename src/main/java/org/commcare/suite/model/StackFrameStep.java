package org.commcare.suite.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.commcare.session.SessionFrame;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapMultiMap;
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
import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

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
     * XML instances collected during session navigation that is made available
     * in the session's evaluation context. For instance, useful to store
     * results of a query command during case search and claim workflow
     */
    private Hashtable<String, ExternalDataInstanceSource> dataInstanceSources = new Hashtable<>();

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

        this.dataInstanceSources.putAll(oldStackFrameStep.dataInstanceSources);
    }

    public StackFrameStep(String type, String id, String value) {
        this.elementType = type;
        this.id = id;
        this.value = value;
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

    public void addDataInstanceSource(ExternalDataInstanceSource source) {
        Objects.requireNonNull(source, String.format(
                "Unable to add null instance data source to stack frame step '%s'", getId()));
        String reference = source.getReference();
        if (dataInstanceSources.containsKey(reference)) {
            throw new RuntimeException(String.format(
                    "Stack frame step '%s' already contains an instance with the reference '%s'",
                    getId(), reference
            ));
        }
        dataInstanceSources.put(reference, source);
    }

    public Hashtable<String, ExternalDataInstanceSource> getDataInstanceSources() {
        return dataInstanceSources;
    }

    public boolean hasDataInstanceSource(String reference) {
        return dataInstanceSources.containsKey(reference);
    }

    public ExternalDataInstanceSource getDataInstanceSource(String reference) {
        return dataInstanceSources.get(reference);
    }

    public ExternalDataInstanceSource getDataInstanceSourceById(String instanceId) {
        for (ExternalDataInstanceSource source : dataInstanceSources.values()) {
            if (source.getInstanceId().equals(instanceId)) {
                return source;
            }
        }
        return null;
    }

    public void initDataInstanceSources(RemoteInstanceFetcher remoteInstanceFetcher)
            throws RemoteInstanceFetcher.RemoteInstanceException {
        for (ExternalDataInstanceSource source : dataInstanceSources.values()) {
            if (source.needsInit()) {
                source.remoteInit(remoteInstanceFetcher, getId());
            }
        }
    }

    public Map<String, DataInstance> getInstances(InstanceInitializationFactory iif) {
        return dataInstanceSources.values().stream().map((source) -> {
            ExternalDataInstance instance = source.toInstance();
            return instance.initialize(iif, source.getInstanceId());
        }).collect(Collectors.toMap(DataInstance::getInstanceId, value -> value));
    }

    /**
     * @param value Must extend Externalizable or be a basic data type (String, Vector, etc)
     */
    public void addExtra(String key, Object value) {
        if (value != null) {
            extras.put(key, value);
        }
    }

    /**
     * Remove all extras for the given key
     *
     * @param key key we want to remove from extras
     */
    public void removeExtra(String key) {
        extras.removeAll(key);
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
            case SessionFrame.STATE_MULTIPLE_DATUM_VAL:
                return new StackFrameStep(SessionFrame.STATE_MULTIPLE_DATUM_VAL, id, evaluateValue(ec));
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
                    if (value instanceof QueryData) {
                        defined.addExtra(key, ((QueryData)value).getValues(ec));
                    } else if (value instanceof XPathExpression) {
                        // only to maintain backward compatibility with old serialised app db state, can be removed in
                        // subsequent deploys
                        defined.addExtra(key, FunctionUtils.toString(((XPathExpression)value).eval(ec)));
                    } else {
                        throw new RuntimeException("Invalid data type for step extra " + key);
                    }
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
        this.dataInstanceSources = (Hashtable<String, ExternalDataInstanceSource>)ExtUtil.read(in, new ExtWrapMap(String.class, ExternalDataInstanceSource.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, elementType);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(id));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(value));
        ExtUtil.writeBool(out, valueIsXpath);
        ExtUtil.write(out, new ExtWrapMultiMap(extras));
        ExtUtil.write(out, new ExtWrapMap(dataInstanceSources));
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
