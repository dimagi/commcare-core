package org.javarosa.core.model.instance;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.model.utils.IInstanceVisitor;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class represents the xform model instance
 */
public class FormInstance extends DataInstance<TreeElement> implements Persistable, IMetaData {

    public static final String STORAGE_KEY = "FORMDATA";
    public static final String META_XMLNS = "XMLNS";
    public static final String META_ID = "instance_id";

    /**
     * The date that this model was taken and recorded
     */
    Date dateSaved;

    public String schema;
    public String formVersion;
    public String uiVersion;

    Hashtable namespaces = new Hashtable();

    /**
     * The root of this tree
     */
    protected TreeElement root = new TreeElement();

    public FormInstance() {
        // for externalization
    }

    public FormInstance(TreeElement root) {
        this(root, null);
    }

    /**
     * Creates a new data model using the root given.
     *
     * @param root The root of the tree for this data model.
     */
    public FormInstance(TreeElement root, String id) {
        super(id);
        setID(-1);
        setFormId(-1);
        setRoot(root);
    }

    @Override
    public TreeElement getBase() {
        return root;
    }

    @Override
    public TreeElement getRoot() {
        if (root.getNumChildren() == 0)
            throw new RuntimeException("root node has no children");

        return root.getChildAt(0);
    }

    /**
     * Sets the root element of this Model's tree
     *
     * @param topLevel root of the tree for this data model.
     */
    public void setRoot(TreeElement topLevel) {
        root = new TreeElement();
        if (this.getInstanceId() != null) {
            root.setInstanceName(this.getInstanceId());
        }
        if (topLevel != null) {
           root.addChild(topLevel);
        }
    }

    public TreeReference copyNode(TreeReference from, TreeReference to) throws InvalidReferenceException {
        if (!from.isAbsolute()) {
            throw new InvalidReferenceException("Source reference must be absolute for copying", from);
        }

        TreeElement src = resolveReference(from);
        if (src == null) {
            throw new InvalidReferenceException("Null Source reference while attempting to copy node", from);
        }

        return copyNode(src, to).getRef();
    }

    // for making new repeat instances; 'from' and 'to' must be unambiguous
    // references EXCEPT 'to' may be ambiguous at its final step
    // return true is successfully copied, false otherwise
    public TreeElement copyNode(TreeElement src, TreeReference to) throws InvalidReferenceException {
        if (!to.isAbsolute())
            throw new InvalidReferenceException("Destination reference must be absolute for copying", to);

        // strip out dest node info and get dest parent
        String dstName = to.getNameLast();
        int dstMult = to.getMultLast();
        TreeReference toParent = to.getParentRef();

        TreeElement parent = resolveReference(toParent);
        if (parent == null) {
            throw new InvalidReferenceException("Null parent reference whle attempting to copy", toParent);
        }
        if (!parent.isChildable()) {
            throw new InvalidReferenceException("Invalid Parent Node: cannot accept children.", toParent);
        }

        if (dstMult == TreeReference.INDEX_UNBOUND) {
            dstMult = parent.getChildMultiplicity(dstName);
        } else if (parent.getChild(dstName, dstMult) != null) {
            throw new InvalidReferenceException("Destination already exists!", to);
        }

        TreeElement dest = src.deepCopy(false);
        dest.setName(dstName);
        dest.setMult(dstMult);
        parent.addChild(dest);
        return dest;
    }

    public Date getDateSaved() {
        return this.dateSaved;
    }

    public void addNamespace(String prefix, String URI) {
        namespaces.put(prefix, URI);
    }

    public String[] getNamespacePrefixes() {
        String[] prefixes = new String[namespaces.size()];
        int i = 0;
        for (Enumeration en = namespaces.keys(); en.hasMoreElements(); ) {
            prefixes[i] = (String)en.nextElement();
            ++i;
        }
        return prefixes;
    }

    public String getNamespaceURI(String prefix) {
        return (String)namespaces.get(prefix);
    }

    @Override
    public FormInstance clone() {
        FormInstance cloned = new FormInstance(this.getRoot().deepCopy(true));

        cloned.setID(this.getID());
        cloned.setFormId(this.getFormId());
        cloned.setName(this.getName());
        cloned.setDateSaved(this.getDateSaved());
        cloned.schema = this.schema;
        cloned.formVersion = this.formVersion;
        cloned.uiVersion = this.uiVersion;
        cloned.namespaces = new Hashtable();
        for (Enumeration e = this.namespaces.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            cloned.namespaces.put(key, this.namespaces.get(key));
        }

        return cloned;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        super.readExternal(in, pf);
        schema = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        dateSaved = (Date)ExtUtil.read(in, new ExtWrapNullable(Date.class), pf);

        namespaces = (Hashtable)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf);
        setRoot((TreeElement)ExtUtil.read(in, TreeElement.class, pf));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.write(out, new ExtWrapNullable(schema));
        ExtUtil.write(out, new ExtWrapNullable(dateSaved));
        ExtUtil.write(out, new ExtWrapMap(namespaces));

        ExtUtil.write(out, getRoot());
    }

    public void setDateSaved(Date dateSaved) {
        this.dateSaved = dateSaved;
    }

    public void copyItemsetNode(TreeElement copyNode, TreeReference destRef, FormDef f)
            throws InvalidReferenceException {
        TreeElement templateNode = getTemplate(destRef);
        TreeElement newNode = copyNode(templateNode, destRef);
        newNode.populateTemplate(copyNode, f);
    }

    public void accept(IInstanceVisitor visitor) {
        visitor.visit(this);

        if (visitor instanceof ITreeVisitor) {
            root.accept((ITreeVisitor)visitor);
        }
    }

    public static boolean isHomogeneous(TreeElement a, TreeElement b) {
        if (a.isLeaf() && b.isLeaf()) {
            return true;
        } else if (a.isChildable() && b.isChildable()) {
            // verify that every (non-repeatable) node in a exists in b and vice
            // versa
            for (int k = 0; k < 2; k++) {
                TreeElement n1 = (k == 0 ? a : b);
                TreeElement n2 = (k == 0 ? b : a);

                for (int i = 0; i < n1.getNumChildren(); i++) {
                    TreeElement child1 = n1.getChildAt(i);
                    if (child1.isRepeatable())
                        continue;
                    TreeElement child2 = n2.getChild(child1.getName(), 0);
                    if (child2 == null)
                        return false;
                    if (child2.isRepeatable())
                        throw new RuntimeException("shouldn't happen");
                }
            }

            // compare children
            for (int i = 0; i < a.getNumChildren(); i++) {
                TreeElement childA = a.getChildAt(i);
                if (childA.isRepeatable())
                    continue;
                TreeElement childB = b.getChild(childA.getName(), 0);
                if (!isHomogeneous(childA, childB))
                    return false;
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public DataInstance initialize(InstanceInitializationFactory initializer, String instanceId) {
        this.instanceid = instanceId;
        root.setInstanceName(instanceId);

        return this;
    }

    @Override
    public String[] getMetaDataFields() {
        return new String[]{META_XMLNS, META_ID};
    }

    /**
     * used by TouchForms
     */
    @SuppressWarnings("unused")
    public Hashtable getMetaData() {
        Hashtable<String, Object> data = new Hashtable<>();
        for (String key : getMetaDataFields()) {
            data.put(key, getMetaData(key));
        }
        return data;
    }

    @Override
    public Object getMetaData(String fieldName) {
        if (META_XMLNS.equals(fieldName)) {
            return ExtUtil.emptyIfNull(schema);
        } else if (META_ID.equals(fieldName)) {
            return ExtUtil.emptyIfNull(this.getInstanceId());
        }
        throw new IllegalArgumentException("No metadata field " + fieldName + " in the form instance storage system");
    }

    /**
     * Custom deserializer for migrating fixtures off of CommCare 2.24.
     *
     * The migration is needed because attribute serialization was redone to
     * capture data-type information.  If this migration is not performed
     * between 2.24 and subsequent versions, fixtures can not be opened. If the
     * migration fails the user can always sync, clear user data, and restore
     * to get reload the fixtures.
     *
     * Used in Android app db migration V.7 and user db migration V.9
     *
     * This can be removed once no devices running 2.24 remain
     */
    public void migrateSerialization(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);
        schema = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        dateSaved = (Date)ExtUtil.read(in, new ExtWrapNullable(Date.class), pf);

        namespaces = (Hashtable)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf);
        TreeElement newRoot;
        try {
            newRoot = TreeElement.class.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } catch (IllegalAccessException e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        newRoot.readExternalMigration(in, pf);
        setRoot(newRoot);
    }
}
