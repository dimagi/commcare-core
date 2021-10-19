package org.javarosa.core.model.instance;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

/**
 * @author ctsims
 */
public class ExternalDataInstance extends DataInstance {
    private String reference;


    private AbstractTreeElement root;
    private InstanceBase base;

    @Nullable
    private ExternalDataInstanceSource source;

    public final static String JR_REMOTE_REFERENCE = "jr://instance/remote";

    public ExternalDataInstance() {
    }

    public ExternalDataInstance(String reference, String instanceid) {
        super(instanceid);
        this.reference = reference;
    }

    /**
     * Copy constructor
     */
    public ExternalDataInstance(ExternalDataInstance instance) {
        super(instance.getInstanceId());
        this.reference = instance.getReference();
        this.base = instance.getBase();
        //Copy constructor avoids check.
        this.root = instance.root;
        this.mCacheHost = instance.getCacheHost();
        this.source = instance.getSource();
    }

    private ExternalDataInstance(String reference, String instanceId,
                                 TreeElement topLevel, ExternalDataInstanceSource source) {
        this(reference, instanceId);
        base = new InstanceBase(instanceId);
        this.source = source;
        topLevel.setInstanceName(instanceId);
        topLevel.setParent(base);
        this.root = topLevel;
        base.setChild(root);
    }

    public static TreeElement parseExternalTree(InputStream stream, String instanceId) throws IOException, UnfullfilledRequirementsException, XmlPullParserException, InvalidStructureException {
        KXmlParser baseParser = ElementParser.instantiateParser(stream);
        TreeElement root = new TreeElementParser(baseParser, 0, instanceId).parse();
        return root;
    }

    public static ExternalDataInstance buildFromRemote(String instanceId,
                                                       ExternalDataInstanceSource source) {
        return new ExternalDataInstance(JR_REMOTE_REFERENCE, instanceId, source.getRoot(), source);
    }

    public boolean useCaseTemplate() {
        return source == null ? CaseInstanceTreeElement.MODEL_NAME.equals(instanceid) : source.useCaseTemplate();
    }

    @Override
    public boolean isRuntimeEvaluated() {
        return true;
    }

    @Override
    public InstanceBase getBase() {
        return base;
    }

    @Override
    public AbstractTreeElement getRoot() {
        if (needsInit()) {
            throw new RuntimeException("Attempt to use instance " + instanceid + " without inititalization.");
        }

        if (source != null) {
            return source.getRoot();
        } else {
            return root;
        }
    }

    public String getReference() {
        return reference;
    }

    @Nullable
    public ExternalDataInstanceSource getSource() {
        return source;
    }

    public boolean needsInit() {
        if (source == null) {
            return false;
        } else {
            return source.needsInit();
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);
        reference = ExtUtil.readString(in);
        source = (ExternalDataInstanceSource)ExtUtil.read(in, new ExtWrapNullable(ExternalDataInstanceSource.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeString(out, reference);
        ExtUtil.write(out, new ExtWrapNullable(source));
    }

    @Override
    public DataInstance initialize(InstanceInitializationFactory initializer, String instanceId) {
        base = new InstanceBase(instanceId);
        root = initializer.generateRoot(this);
        base.setChild(root);
        return initializer.getSpecializedExternalDataInstance(this);
    }
}
