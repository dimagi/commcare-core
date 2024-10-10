package org.javarosa.model.xform;

import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeReference;
import org.kxml2.io.KXmlSerializer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A quick rewrite of the basics for writing higher level xml documents straight to
 * output streams.
 *
 * @author Clayton Sims
 */
public class DataModelSerializer {

    private final KXmlSerializer serializer;
    private final InstanceInitializationFactory factory;

    public DataModelSerializer(OutputStream stream, InstanceInitializationFactory factory) throws IOException {
        serializer = new KXmlSerializer();
        serializer.setOutput(stream, "UTF-8");
        this.factory = factory;
    }

    public DataModelSerializer(KXmlSerializer serializer) {
        this.serializer = serializer;
        this.factory = null;
    }

    public void serialize(ExternalDataInstance instance, TreeReference base) throws IOException {
        DataInstance specializedInstance = instance.initialize(factory, instance.getInstanceId());
        serialize(specializedInstance, base);
    }

    public void serialize(DataInstance instance, TreeReference base) throws IOException {
        //TODO: Namespaces?
        AbstractTreeElement root;
        if (base == null) {
            root = instance.getRoot();
        } else {
            root = instance.resolveReference(base);
        }
        serialize(root);
    }

    public void serialize(AbstractTreeElement root) throws IOException {
        serializer.startTag(root.getNamespace(), root.getName());

        serializeAttributes(root);
        for (int i = 0; i < root.getNumChildren(); i++) {
            AbstractTreeElement childAt = root.getChildAt(i);
            serializeNode(childAt);
        }

        serializer.endTag(root.getNamespace(), root.getName());
        serializer.flush();
    }

    private void serializeNode(AbstractTreeElement instanceNode) throws IOException {
        //don't serialize template nodes or non-relevant nodes
        if (!instanceNode.isRelevant() || instanceNode.getMult() == TreeReference.INDEX_TEMPLATE) {
            return;
        }

        serializer.startTag(instanceNode.getNamespace(), instanceNode.getName());
        serializeAttributes(instanceNode);

        if (instanceNode.getValue() != null) {
            serializer.text(instanceNode.getValue().uncast().getString());
        } else {
            for (int i = 0; i < instanceNode.getNumChildren(); ++i) {
                serializeNode(instanceNode.getChildAt(i));
            }
        }

        serializer.endTag(instanceNode.getNamespace(), instanceNode.getName());
    }

    private void serializeAttributes(AbstractTreeElement instanceNode) throws IOException {
        for (int i = 0; i < instanceNode.getAttributeCount(); ++i) {
            String val = instanceNode.getAttributeValue(i);
            val = val == null ? "" : val;
            serializer.attribute(instanceNode.getAttributeNamespace(i), instanceNode.getAttributeName(i), val);
        }
    }
}