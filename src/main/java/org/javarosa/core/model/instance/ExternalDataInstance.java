package org.javarosa.core.model.instance;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.core.interfaces.RemoteInstanceFetcher;
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
    private ExternalDataInstanceSource source;

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
        //Copy constructor avoids check.
        this.root = instance.root;
        this.mCacheHost = instance.getCacheHost();
        this.source = instance.getSource();
        useCaseTemplate = CaseInstanceTreeElement.MODEL_NAME.equals(instanceid);
    }

    private ExternalDataInstance(String reference, String instanceId,
                                 TreeElement topLevel, ExternalDataInstanceSource source,
                                 boolean useCaseTemplate) {
        this(reference, instanceId);
        this.useCaseTemplate = useCaseTemplate;
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
                                                       ExternalDataInstanceSource source,
                                                       boolean useCaseTemplate) {
        return new ExternalDataInstance(JR_REMOTE_REFERENCE, instanceId, source.getRoot(), source, useCaseTemplate);
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

    public void remoteInit(RemoteInstanceFetcher remoteInstanceFetcher)
            throws RemoteInstanceFetcher.RemoteInstanceException {
        try {
            base = new InstanceBase(getInstanceId());
            this.source.init(remoteInstanceFetcher.getExternalRoot(getInstanceId(), this.getSource()));
            TreeElement root = source.getRoot();
            root.setInstanceName(this.instanceid);
            root.setParent(this.base);
            this.root = root;
        } catch (IOException e) {
            throw new RemoteInstanceFetcher.RemoteInstanceException(
                    "Could not retrieve data for remote instance " + getName() + ". Please try opening the form again.", e);
        } catch (XmlPullParserException | UnfullfilledRequirementsException | InvalidStructureException e) {
            throw new RemoteInstanceFetcher.RemoteInstanceException(
                    "Invalid data retrieved from remote instance " + getName() + ". If the error persists please contact your help desk.", e);
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);
        reference = ExtUtil.readString(in);
        useCaseTemplate = ExtUtil.readBool(in);
        source = (ExternalDataInstanceSource)ExtUtil.read(in, new ExtWrapNullable(ExternalDataInstanceSource.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeString(out, reference);
        ExtUtil.writeBool(out, useCaseTemplate);
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
