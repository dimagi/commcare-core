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
    private boolean useCaseTemplate;

    private AbstractTreeElement root;
    private InstanceBase base;

    @Nullable
    private String remoteUrl;

    public final static String JR_REMOTE_REFERENCE = "jr://instance/remote";

    public ExternalDataInstance() {
    }

    public ExternalDataInstance(String reference, String instanceid) {
        super(instanceid);
        this.reference = reference;
        useCaseTemplate = CaseInstanceTreeElement.MODEL_NAME.equals(instanceid);
    }

    /**
     * Copy constructor
     */
    public ExternalDataInstance(ExternalDataInstance instance) {
        super(instance.getInstanceId());
        this.reference = instance.getReference();
        this.base = instance.getBase();
        this.root = instance.getRoot();
        this.mCacheHost = instance.getCacheHost();
        this.remoteUrl = instance.getRemoteUrl();
        useCaseTemplate = CaseInstanceTreeElement.MODEL_NAME.equals(instanceid);
    }

    private ExternalDataInstance(String reference, String instanceId,
                                 TreeElement topLevel, String remoteUrl, boolean useCaseTemplate) {
        this(reference, instanceId);
        this.useCaseTemplate = useCaseTemplate;
        this.remoteUrl = remoteUrl;
        base = new InstanceBase(instanceId);
        topLevel.setInstanceName(instanceId);
        topLevel.setParent(base);
        this.root = topLevel;
        base.setChild(root);
    }

    public static ExternalDataInstance buildFromRemote(String instanceId,
                                                       TreeElement root,
                                                       String remoteUrl,
                                                       boolean useCaseTemplate) {
        return new ExternalDataInstance(JR_REMOTE_REFERENCE, instanceId, root, remoteUrl, useCaseTemplate);
    }

    public static ExternalDataInstance buildFromRemote(String instanceId,
                                                       InputStream instanceStream,
                                                       String remoteUrl,
                                                       boolean useCaseTemplate)
            throws IOException, UnfullfilledRequirementsException, XmlPullParserException, InvalidStructureException {
        KXmlParser baseParser = ElementParser.instantiateParser(instanceStream);
        TreeElement root = new TreeElementParser(baseParser, 0, instanceId).parse();
        return new ExternalDataInstance(JR_REMOTE_REFERENCE, instanceId, root, remoteUrl, useCaseTemplate);
    }

    public boolean useCaseTemplate() {
        return useCaseTemplate;
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
        return root;
    }

    public String getReference() {
        return reference;
    }

    @Nullable
    public String getRemoteUrl() {
        return remoteUrl;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);
        reference = ExtUtil.readString(in);
        useCaseTemplate = ExtUtil.readBool(in);
        remoteUrl = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeString(out, reference);
        ExtUtil.writeBool(out, useCaseTemplate);
        ExtUtil.write(out, new ExtWrapNullable(remoteUrl));
    }

    @Override
    public DataInstance initialize(InstanceInitializationFactory initializer, String instanceId) {
        base = new InstanceBase(instanceId);
        root = initializer.generateRoot(this);

        if (root == null) {
            ExternalDataInstance sessionInstance = initializer.getInstanceFromSession(instanceId);
            reference = sessionInstance.getReference();
            base = sessionInstance.getBase();
            root = sessionInstance.getRoot();
            mCacheHost = sessionInstance.getCacheHost();
            remoteUrl = sessionInstance.getRemoteUrl();
            useCaseTemplate = CaseInstanceTreeElement.MODEL_NAME.equals(instanceid);
        }

        if (root == null) {
            initializer.generateRoot(this);
        }

        if (root != null) {
            base.setChild(root);
        }

        return initializer.getSpecializedExternalDataInstance(this);
    }
}
