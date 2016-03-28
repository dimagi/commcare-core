package org.javarosa.model.xform;

import org.javarosa.core.data.IDataPointer;
import org.javarosa.core.model.IAnswerDataSerializer;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.IInstanceSerializingVisitor;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.core.services.transport.payload.DataPointerPayload;
import org.javarosa.core.services.transport.payload.IDataPayload;
import org.javarosa.core.services.transport.payload.MultiMessagePayload;
import org.javarosa.xform.util.XFormAnswerDataSerializer;
import org.javarosa.xform.util.XFormSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A visitor-esque class which walks a FormInstance and constructs an XML document
 * containing its instance.
 *
 * The XML node elements are constructed in a depth-first manner, consistent with
 * standard XML document parsing.
 *
 * @author Clayton Sims
 */
public class XFormSerializingVisitor implements IInstanceSerializingVisitor {

    /**
     * The XML document containing the instance that is to be returned
     */
    Document theXmlDoc;

    /**
     * The serializer to be used in constructing XML for AnswerData elements
     */
    IAnswerDataSerializer serializer;

    /**
     * The root of the xml document which should be included in the serialization *
     */
    TreeReference rootRef;

    Vector<IDataPointer> dataPointers;

    boolean respectRelevance = true;

    public XFormSerializingVisitor() {
        this(true);
    }

    public XFormSerializingVisitor(boolean respectRelevance) {
        this.respectRelevance = respectRelevance;
    }

    private void init() {
        theXmlDoc = null;
        dataPointers = new Vector<IDataPointer>();
    }

    @Override
    public byte[] serializeInstance(FormInstance model) throws IOException {
        return serializeInstance(model, new XPathReference("/"));
    }

    @Override
    public byte[] serializeInstance(FormInstance model, XPathReference ref) throws IOException {
        init();
        rootRef = FormInstance.unpackReference(ref);
        if (this.serializer == null) {
            this.setAnswerDataSerializer(new XFormAnswerDataSerializer());
        }

        model.accept(this);
        if (theXmlDoc != null) {
            return XFormSerializer.getUtfBytes(theXmlDoc);
        } else {
            return null;
        }
    }

    @Override
    public IDataPayload createSerializedPayload(FormInstance model) throws IOException {
        return createSerializedPayload(model, new XPathReference("/"));
    }

    @Override
    public IDataPayload createSerializedPayload(FormInstance model, XPathReference ref) throws IOException {
        init();
        rootRef = FormInstance.unpackReference(ref);
        if (this.serializer == null) {
            this.setAnswerDataSerializer(new XFormAnswerDataSerializer());
        }
        model.accept(this);
        if (theXmlDoc != null) {
            //TODO: Did this strip necessary data?
            byte[] form = XFormSerializer.getUtfBytes(theXmlDoc);
            if (dataPointers.size() == 0) {
                return new ByteArrayPayload(form, null, IDataPayload.PAYLOAD_TYPE_XML);
            }
            MultiMessagePayload payload = new MultiMessagePayload();
            payload.addPayload(new ByteArrayPayload(form, "xml_submission_file", IDataPayload.PAYLOAD_TYPE_XML));
            Enumeration en = dataPointers.elements();
            while (en.hasMoreElements()) {
                IDataPointer pointer = (IDataPointer)en.nextElement();
                payload.addPayload(new DataPointerPayload(pointer));
            }
            return payload;
        } else {
            return null;
        }
    }

    @Override
    public void visit(FormInstance tree) {
        theXmlDoc = new Document();

        TreeElement root = tree.resolveReference(rootRef);

        //For some reason resolveReference won't ever return the root, so we'll
        //catch that case and just start at the root.
        if (root == null) {
            root = tree.getRoot();
        }

        if (root != null) {
            theXmlDoc.addChild(Node.ELEMENT, serializeNode(root));
        }

        Element top = theXmlDoc.getElement(0);

        String[] prefixes = tree.getNamespacePrefixes();
        for (String prefix : prefixes) {
            top.setPrefix(prefix, tree.getNamespaceURI(prefix));
        }
        if (tree.schema != null) {
            top.setNamespace(tree.schema);
            top.setPrefix("", tree.schema);
        }
    }

    private Element serializeNode(TreeElement instanceNode) {
        Element e = new Element(); //don't set anything on this element yet, as it might get overwritten

        //don't serialize template nodes or non-relevant nodes
        if ((respectRelevance && !instanceNode.isRelevant()) || instanceNode.getMult() == TreeReference.INDEX_TEMPLATE) {
            return null;
        }

        if (instanceNode.getValue() != null) {
            Object serializedAnswer = serializer.serializeAnswerData(instanceNode.getValue(), instanceNode.getDataType());

            if (serializedAnswer instanceof Element) {
                e = (Element)serializedAnswer;
            } else if (serializedAnswer instanceof String) {
                e = new Element();
                e.addChild(Node.TEXT, serializedAnswer);
            } else {
                throw new RuntimeException("Can't handle serialized output for" + instanceNode.getValue().toString() + ", " + serializedAnswer);
            }

            if (serializer.containsExternalData(instanceNode.getValue()).booleanValue()) {
                IDataPointer[] pointers = serializer.retrieveExternalDataPointer(instanceNode.getValue());
                for (IDataPointer pointer : pointers) {
                    dataPointers.addElement(pointer);
                }
            }
        } else {
            //make sure all children of the same tag name are written en bloc
            Vector<String> childNames = new Vector<String>();
            for (int i = 0; i < instanceNode.getNumChildren(); i++) {
                String childName = instanceNode.getChildAt(i).getName();
                if (!childNames.contains(childName))
                    childNames.addElement(childName);
            }

            for (int i = 0; i < childNames.size(); i++) {
                String childName = childNames.elementAt(i);
                int mult = instanceNode.getChildMultiplicity(childName);
                for (int j = 0; j < mult; j++) {
                    Element child = serializeNode(instanceNode.getChild(childName, j));
                    if (child != null) {
                        e.addChild(Node.ELEMENT, child);
                    }
                }
            }
        }

        e.setName(instanceNode.getName());

        // add hard-coded attributes
        for (int i = 0; i < instanceNode.getAttributeCount(); i++) {
            String namespace = instanceNode.getAttributeNamespace(i);
            String name = instanceNode.getAttributeName(i);
            String val = instanceNode.getAttributeValue(i);
            // is it legal for getAttributeValue() to return null? playing it safe for now and assuming yes
            if (val == null) {
                val = "";
            }
            e.setAttribute(namespace, name, val);
        }
        if (instanceNode.getNamespace() != null) {
            e.setNamespace(instanceNode.getNamespace());
        }

        return e;
    }

    @Override
    public void setAnswerDataSerializer(IAnswerDataSerializer ads) {
        this.serializer = ads;
    }

    @Override
    public IInstanceSerializingVisitor newInstance() {
        XFormSerializingVisitor modelSerializer = new XFormSerializingVisitor();
        modelSerializer.setAnswerDataSerializer(this.serializer);
        return modelSerializer;
    }
}
