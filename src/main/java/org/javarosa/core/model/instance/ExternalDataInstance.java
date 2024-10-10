package org.javarosa.core.model.instance;

import static org.javarosa.core.model.instance.utils.InstanceUtils.setUpInstanceRoot;

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

    public static final String JR_SESSION_REFERENCE = "jr://instance/session";
    public static final String JR_CASE_DB_REFERENCE = "jr://instance/casedb";
    public static final String JR_LEDGER_DB_REFERENCE = "jr://instance/ledgerdb";
    public static final String JR_SEARCH_INPUT_REFERENCE = "jr://instance/search-input";
    public static final String JR_SELECTED_ENTITIES_REFERENCE = "jr://instance/selected-entities";
    public static final String JR_REMOTE_REFERENCE = "jr://instance/remote";

    private String reference;
    private AbstractTreeElement root;
    private InstanceBase base;

    @Nullable
    private ExternalDataInstanceSource source;

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

    public ExternalDataInstance(String reference, String instanceId,
            TreeElement topLevel) {
        this(reference, instanceId, topLevel, null);
    }

    public ExternalDataInstance(String reference, String instanceId,
            AbstractTreeElement topLevel, ExternalDataInstanceSource source) {
        this(reference, instanceId);
        base = new InstanceBase(instanceId);
        this.source = source;
        this.root = topLevel;
        setUpInstanceRoot(root, instanceId, base);
        base.setChild(root);
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
        source = (ExternalDataInstanceSource)ExtUtil.read(in,
                new ExtWrapNullable(ExternalDataInstanceSource.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeString(out, reference);
        ExtUtil.write(out, new ExtWrapNullable(source));
    }

    @Override
    public DataInstance initialize(InstanceInitializationFactory initializer, String instanceId, String locale) {
        base = new InstanceBase(instanceId);
        InstanceRoot instanceRoot = initializer.generateRoot(this, locale);
        // this indirectly calls `this.copyFromSource` via the InstanceRoot so that we call the
        // correct method based on the type
        instanceRoot.setupNewCopy(this);
        return initializer.getSpecializedExternalDataInstance(this);
    }

    public DataInstance initialize(InstanceInitializationFactory initializer, String instanceId) {
        return initialize(initializer, instanceId, null);
    }

    public void copyFromSource(InstanceRoot instanceRoot) {
        root = instanceRoot.getRoot();
        base.setChild(root);
    }

    public void copyFromSource(ExternalDataInstanceSource source) {
        //parent copy
        copyFromSource((InstanceRoot)source);
        this.source = source;
    }

    /**
     * Copy method to allow creating copies of this instance without having to know
     * what the instance class is.
     */
    public ExternalDataInstance copy() {
        return new ExternalDataInstance(this);
    }
}
