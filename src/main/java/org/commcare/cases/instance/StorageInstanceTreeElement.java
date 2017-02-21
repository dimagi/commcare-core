package org.commcare.cases.instance;

import org.commcare.cases.util.StorageBackedTreeRoot;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.Interner;
import org.javarosa.core.util.externalizable.Externalizable;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Instance root for storage-backed instances such as the case and ledger DBs
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public abstract class StorageInstanceTreeElement<Model extends Externalizable, T extends AbstractTreeElement>
        extends StorageBackedTreeRoot<T> {

    private String modelName;
    private String childName;

    private AbstractTreeElement instanceRoot;

    protected final IStorageUtilityIndexed<Model> storage;
    protected Vector<T> elements;
    protected final Interner<TreeElement> treeCache = new Interner<>();
    private Interner<String> stringCache = new Interner<>();

    private int numRecords = -1;
    private TreeReference cachedRef = null;

    public StorageInstanceTreeElement(AbstractTreeElement instanceRoot,
                                      IStorageUtilityIndexed<Model> storage,
                                      String modelName, String childName) {
        this.instanceRoot = instanceRoot;
        this.storage = storage;
        this.modelName = modelName;
        this.childName = childName;
    }

    /**
     * Rebase assigns this tree element to a new root instance node.
     *
     * Used to migrate the already created tree structure to a new instance connector.
     *
     * @param instanceRoot The root of the new tree that this element should be a part of
     */
    public void rebase(AbstractTreeElement instanceRoot) {
        this.instanceRoot = instanceRoot;
        expireCachedRef();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public boolean isChildable() {
        return false;
    }

    @Override
    public String getInstanceName() {
        return instanceRoot.getInstanceName();
    }

    @Override
    public T getChild(String name, int multiplicity) {
        if ((multiplicity == TreeReference.INDEX_TEMPLATE) &&
                childName.equals(name)) {
            return getChildTemplate();
        }

        //name is always "case", so multiplicities are the only relevant component here
        if (childName.equals(name)) {
            loadElements();
            if (elements.isEmpty()) {
                //If we have no cases, we still need to be able to return a template element so as to not
                //break xpath evaluation
                return getChildTemplate();
            }
            return elements.elementAt(multiplicity);
        }
        return null;
    }

    protected synchronized void loadElements() {
        if (elements != null) {
            return;
        }
        objectIdMapping = new Hashtable<>();
        elements = new Vector<>();
        int mult = 0;
        for (IStorageIterator i = storage.iterate(); i.hasMore(); ) {
            int id = i.nextID();
            elements.add(buildElement(this, id, null, mult));
            objectIdMapping.put(DataUtil.integer(id), DataUtil.integer(mult));
            mult++;
        }
    }

    @Override
    public Vector<T> getChildrenWithName(String name) {
        if (name.equals(childName)) {
            loadElements();
            return elements;
        } else {
            return new Vector<>();
        }
    }

    @Override
    public boolean hasChildren() {
        return getNumChildren() > 0;
    }

    @Override
    public int getNumChildren() {
        if (numRecords == -1) {
            numRecords = storage.getNumRecords();
        }
        return numRecords;
    }

    @Override
    public T getChildAt(int i) {
        loadElements();
        return elements.elementAt(i);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public boolean isAttribute() {
        return false;
    }

    @Override
    public int getChildMultiplicity(String name) {
        //All children have the same name;
        if (name.equals(childName)) {
            return this.getNumChildren();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isRelevant() {
        return true;
    }

    @Override
    public void accept(ITreeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int getAttributeCount() {
        return 0;
    }

    @Override
    public String getAttributeNamespace(int index) {
        return null;
    }

    @Override
    public String getAttributeName(int index) {
        return null;
    }

    @Override
    public String getAttributeValue(int index) {
        return null;
    }

    @Override
    public T getAttribute(String namespace, String name) {
        return null;
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        return null;
    }

    @Override
    public TreeReference getRef() {
        if (cachedRef == null) {
            cachedRef = TreeReference.buildRefFromTreeElement(this);
        }
        return cachedRef;
    }

    private void expireCachedRef() {
        cachedRef = null;
    }

    @Override
    public String getName() {
        return modelName;
    }

    @Override
    public int getMult() {
        return 0;
    }

    @Override
    public AbstractTreeElement getParent() {
        return instanceRoot;
    }

    @Override
    public IAnswerData getValue() {
        return null;
    }

    @Override
    public int getDataType() {
        return 0;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    protected String getChildHintName() {
        return childName;
    }

    @Override
    protected IStorageUtilityIndexed<?> getStorage() {
        return storage;
    }

    @Override
    protected void initStorageCache() {
        loadElements();
    }

    public void attachStringCache(Interner<String> stringCache) {
        this.stringCache = stringCache;
    }

    public String intern(String s) {
        if (stringCache == null) {
            return s;
        } else {
            return stringCache.intern(s);
        }
    }

    protected abstract T buildElement(StorageInstanceTreeElement<Model, T> storageInstance,
                                      int recordId, String id, int mult);

    protected Model getElement(int recordId) {
        return storage.read(recordId);
    }

    protected Model getModelTemplate() {
        return storage.read(0);
    }

    protected abstract T getChildTemplate();
}
