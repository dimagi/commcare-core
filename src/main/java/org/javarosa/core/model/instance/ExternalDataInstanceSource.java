package org.javarosa.core.model.instance;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.javarosa.core.util.externalizable.*;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.expr.XPathExpression;
import org.xmlpull.v1.XmlPullParserException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Wrapper class for remote data instances which will materialize the instance data
 * from the source information when needed.
 */
public class ExternalDataInstanceSource implements InstanceRoot, Externalizable {

    TreeElement root;
    private String sourceUri;
    private String instanceId;
    private Multimap<String, String> requestData;
    private boolean useCaseTemplate;

    /**
     * Externalizable constructor
     */
    public ExternalDataInstanceSource() {}

    public ExternalDataInstanceSource(String instanceId, String sourceUri, Multimap<String, String> requestData, boolean useCaseTemplate) {
        this(instanceId, null, sourceUri, requestData, useCaseTemplate);
    }

    public ExternalDataInstanceSource(String instanceId, TreeElement root, String sourceUri, boolean useCaseTemplate) {
        this(instanceId, root, sourceUri, ImmutableMultimap.of(), useCaseTemplate);
    }

    public ExternalDataInstanceSource(String instanceId, TreeElement root, String sourceUri, Multimap<String, String> requestData, boolean useCaseTemplate) {
        this.instanceId = instanceId;
        this.sourceUri = sourceUri;
        this.requestData = requestData;
        this.root = root;
        this.useCaseTemplate = useCaseTemplate;
    }

    /**
     * Copy constructor
     */
    public ExternalDataInstanceSource(ExternalDataInstanceSource externalDataInstanceSource)  {
        this.instanceId = externalDataInstanceSource.instanceId;
        this.sourceUri = externalDataInstanceSource.sourceUri;
        this.root = externalDataInstanceSource.root;
        this.useCaseTemplate = externalDataInstanceSource.useCaseTemplate();
        this.requestData = externalDataInstanceSource.requestData;
    }

    public boolean needsInit() {
        if (root == null) {
            return true;
        }
        return false;
    }

    public TreeElement getRoot() {
        if (needsInit()) {
            throw new RuntimeException("Uninstantiated external instance source");
        }
        return root;
    }

    public void init(TreeElement root) {
        if (this.root != null) {
            throw new RuntimeException("Initializing an already instantiated external instance source is not permitted");
        }
        this.root = root;
    }

    public void remoteInit(RemoteInstanceFetcher remoteInstanceFetcher) throws RemoteInstanceFetcher.RemoteInstanceException {
        try {
            init(remoteInstanceFetcher.getExternalRoot(instanceId, this));
            root.setInstanceName(instanceId);
            root.setParent(new InstanceBase(instanceId));
        } catch (IOException e) {
            throw new RemoteInstanceFetcher.RemoteInstanceException(
                    "Could not retrieve data for remote instance " + instanceId + ". Please try opening the form again.", e);
        } catch (XmlPullParserException | UnfullfilledRequirementsException | InvalidStructureException e) {
            throw new RemoteInstanceFetcher.RemoteInstanceException(
                    "Invalid data retrieved from remote instance " + instanceId+ ". If the error persists please contact your help desk.", e);
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        sourceUri = ExtUtil.readString(in);
        instanceId = ExtUtil.readString(in);
        useCaseTemplate = ExtUtil.readBool(in);
        requestData = (Multimap<String, String>)ExtUtil.read(in, new ExtWrapMultiMap(String.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, sourceUri);
        ExtUtil.write(out, instanceId);
        ExtUtil.writeBool(out, useCaseTemplate);
        ExtUtil.write(out, new ExtWrapMultiMap(requestData));
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public boolean useCaseTemplate() {
        return useCaseTemplate;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public Multimap<String, String> getRequestData() {
        return requestData;
    }

    public void setupNewCopy(ExternalDataInstance instance) {
        instance.copyFromSource(this);
    }
}
