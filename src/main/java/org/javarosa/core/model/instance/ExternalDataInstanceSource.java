package org.javarosa.core.model.instance;

import static org.javarosa.core.model.instance.ExternalDataInstance.JR_REMOTE_REFERENCE;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMultiMap;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

/**
 * Wrapper class for remote data instances which will materialize the instance data
 * from the source information when needed.
 */
public class ExternalDataInstanceSource implements InstanceRoot, Externalizable {

    @Nullable
    private AbstractTreeElement root;
    private String instanceId;
    private boolean useCaseTemplate;
    private String reference;

    @Nullable
    private String sourceUri;
    private Multimap<String, String> requestData;

    @Nullable
    private String storageReferenceId;

    public ExternalDataInstanceSource() {
    }

    private ExternalDataInstanceSource(String instanceId, TreeElement root, String reference,
            boolean useCaseTemplate,
            String sourceUri, Multimap<String, String> requestData, String storageReferenceId) {
        if (sourceUri == null && storageReferenceId == null) {
            throw new RuntimeException(getClass().getCanonicalName()
                    + " must be initialised with one of sourceUri or storageReferenceId");
        }
        this.instanceId = instanceId;
        this.root = root;
        this.reference = reference;
        this.useCaseTemplate = useCaseTemplate;
        this.sourceUri = sourceUri;
        this.requestData = requestData;
        this.storageReferenceId = storageReferenceId;
    }

    /**
     * Copy constructor
     */
    public ExternalDataInstanceSource(ExternalDataInstanceSource externalDataInstanceSource) {
        this.instanceId = externalDataInstanceSource.instanceId;
        this.root = externalDataInstanceSource.root;
        this.reference = externalDataInstanceSource.reference;
        this.useCaseTemplate = externalDataInstanceSource.useCaseTemplate();
        this.sourceUri = externalDataInstanceSource.sourceUri;
        this.requestData = externalDataInstanceSource.requestData;
        this.storageReferenceId = externalDataInstanceSource.storageReferenceId;
    }

    public static ExternalDataInstanceSource buildRemote(
            String instanceId, @Nullable TreeElement root,
            boolean useCaseTemplate, String sourceUri,
            Multimap<String, String> requestData) {
        return new ExternalDataInstanceSource(instanceId, root, getRemoteReference(instanceId),
                useCaseTemplate, sourceUri, requestData, null);
    }

    private static String getRemoteReference(String instanceId) {
        return JR_REMOTE_REFERENCE.concat("/").concat(instanceId);
    }

    public static ExternalDataInstanceSource buildVirtual(
            ExternalDataInstance instance, String storageReferenceId) {
        return buildVirtual(
                instance.getInstanceId(),
                (TreeElement)instance.getRoot(),
                instance.getReference(),
                instance.useCaseTemplate(),
                storageReferenceId
        );
    }

    public static ExternalDataInstanceSource buildVirtual(
            String instanceId, @Nullable TreeElement root,
            String reference, boolean useCaseTemplate,
            String storageReferenceId) {
        return new ExternalDataInstanceSource(instanceId, root, reference,
                useCaseTemplate, null, ImmutableMultimap.of(), storageReferenceId);
    }

    public boolean needsInit() {
        if (root == null) {
            return true;
        }
        return false;
    }

    public AbstractTreeElement getRoot() {
        if (needsInit()) {
            throw new RuntimeException("Uninstantiated external instance source");
        }
        return root;
    }

    public void init(AbstractTreeElement root) {
        if (this.root != null) {
            throw new RuntimeException(
                    "Initializing an already instantiated external instance source is not permitted");
        }
        this.root = root;
    }

    public void remoteInit(RemoteInstanceFetcher remoteInstanceFetcher)
            throws RemoteInstanceFetcher.RemoteInstanceException {
        String instanceId = getInstanceId();
        init(remoteInstanceFetcher.getExternalRoot(instanceId, this));
        if (root instanceof TreeElement) {
            TreeElement rootAsTreeElement = ((TreeElement)root);
            rootAsTreeElement.setInstanceName(instanceId);
            rootAsTreeElement.setParent(new InstanceBase(instanceId));
        }
    }

    public void setupNewCopy(ExternalDataInstance instance) {
        instance.copyFromSource(this);
    }

    public ExternalDataInstance toInstance() {
        return new ExternalDataInstance(getReference(), getInstanceId(), getRoot(), this);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        instanceId = ExtUtil.readString(in);
        useCaseTemplate = ExtUtil.readBool(in);
        sourceUri = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        requestData = (Multimap<String, String>)ExtUtil.read(in, new ExtWrapMultiMap(String.class), pf);
        storageReferenceId = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        reference = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, instanceId);
        ExtUtil.writeBool(out, useCaseTemplate);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(sourceUri));
        ExtUtil.write(out, new ExtWrapMultiMap(requestData));
        ExtUtil.write(out, new ExtWrapNullable(storageReferenceId == null ? null : storageReferenceId.toString()));
        ExtUtil.writeString(out, reference);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public boolean useCaseTemplate() {
        return useCaseTemplate;
    }

    public String getReference() {
        return reference;
    }

    @Nullable
    public String getSourceUri() {
        return sourceUri;
    }

    public Multimap<String, String> getRequestData() {
        return requestData;
    }

    @Nullable
    public String getStorageReferenceId() {
        return storageReferenceId;
    }
}
