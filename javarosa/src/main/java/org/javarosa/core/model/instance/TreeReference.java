package org.javarosa.core.model.instance;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;


// TODO: This class needs to be immutable so that we can perform caching
// optimizations.
public class TreeReference implements Externalizable {

    int hashCode = -1;

    // Multiplicity demarcates the position of a given element with respect to
    // other elements of the same name.

    // Since users usually want to select the first instance from the nodeset
    // returned from a reference query, let the default multiplicity be
    // selecting the first node.
    public static final int DEFAULT_MUTLIPLICITY = 0;

    // refers to all instances of an element, e.g. /data/b[-1] refers to b[0]
    // and b[1]
    public static final int INDEX_UNBOUND = -1;

    // 'repeats' (sections of a form that can multiply themselves) are
    // populated with a template that never exists in the form (IE: If you
    // serialized the form to XML it wouldn't be there) but provides the xml
    // structure that should be replicated when a 'repeat' is added
    public static final int INDEX_TEMPLATE = -2;

    // multiplicity flag for an attribute
    public static final int INDEX_ATTRIBUTE = -4;

    public static final int INDEX_REPEAT_JUNCTURE = -10;


    //TODO: Roll these into RefLevel? Or more likely, take absolute
    //ref out of refLevel
    public static final int CONTEXT_ABSOLUTE = 0;
    // context is inherited since the path is relative
    public static final int CONTEXT_INHERITED = 1;
    // use the original context instead of current context, used by the
    // current() command.
    public static final int CONTEXT_ORIGINAL = 2;
    public static final int CONTEXT_INSTANCE = 4;

    public static final int REF_ABSOLUTE = -1;

    public static final String NAME_WILDCARD = "*";

    // -1 = absolute, 0 = context node, 1 = parent, 2 = grandparent ...
    private int refLevel;
    private int contextType;

    /**
     * Name of the reference's root, if it is a non-main instance, otherwise
     * null.
     */
    private String instanceName = null;

    private Vector<TreeReferenceLevel> data = null;

    // This value will be computed lazily during calls to size(); every time
    // 'data' changes size, set it to -1 and compute it on demand.
    int size = -1;

    public TreeReference() {
        instanceName = null;
        data = new Vector<>();
    }

    public TreeReference(String instanceName, int refLevel) {
        this(instanceName, refLevel, -1);
    }

    private TreeReference(String instanceName, int refLevel, int contextType) {
        this.instanceName = instanceName;
        this.refLevel = refLevel;
        this.contextType = contextType;
        this.data = new Vector<>();
        setupContextTypeFromInstanceName();
    }

    private void setupContextTypeFromInstanceName() {
        if (this.instanceName == null) {
            if (this.refLevel == REF_ABSOLUTE) {
                this.contextType = CONTEXT_ABSOLUTE;
            } else {
                this.contextType = CONTEXT_INHERITED;
            }
        } else {
            this.contextType = CONTEXT_INSTANCE;
        }
    }

    /**
     * Build a '/' reference
     *
     * @return a reference that represents a root/'/' path
     */
    public static TreeReference rootRef() {
        return new TreeReference(null, REF_ABSOLUTE, CONTEXT_ABSOLUTE);
    }

    /**
     * Build a '.' reference
     *
     * @return a reference that represents a self/'.' path
     */
    public static TreeReference selfRef() {
        return new TreeReference(null, 0, CONTEXT_INHERITED);
    }

    /**
     * Build a 'current()' reference
     *
     * @return a reference that represents a base 'current()' path
     */
    public static TreeReference baseCurrentRef() {
        TreeReference currentRef = new TreeReference(null, 0, CONTEXT_ORIGINAL);
        // TODO PLM, make this unneeded
        currentRef.contextType = CONTEXT_ORIGINAL;
        return currentRef;
    }

    public static TreeReference buildRefFromTreeElement(AbstractTreeElement elem) {
        TreeReference ref = TreeReference.selfRef();

        while (elem != null) {
            TreeReference step;

            if (elem.getName() != null) {
                step = new TreeReference(elem.getInstanceName(), 0, CONTEXT_INHERITED);
                step.add(elem.getName(), elem.getMult());
            } else {
                //All TreeElements are part of a consistent tree, so the root should be in the same instance
                step = new TreeReference(elem.getInstanceName(), REF_ABSOLUTE, CONTEXT_ABSOLUTE);
            }

            ref = ref.parent(step);
            elem = elem.getParent();
        }
        return ref;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public int getMultiplicity(int index) {
        return data.elementAt(index).getMultiplicity();
    }

    public String getName(int index) {
        return data.elementAt(index).getName();
    }

    public int getMultLast() {
        return data.lastElement().getMultiplicity();
    }

    public String getNameLast() {
        return data.lastElement().getName();
    }

    public void setMultiplicity(int i, int mult) {
        hashCode = -1;
        data.setElementAt(data.elementAt(i).setMultiplicity(mult), i);
    }

    /**
     * How many reference levels are present? Compute this value on demand and
     * cache it.
     *
     * @return the number of reference levels
     */
    public int size() {
        // csims@dimagi.com - this seems unecessary but is a shocking
        // performance difference due to the number of high-churn circumstances
        // where this call is made.
        if (size == -1) {
            size = data.size();
        }
        return size;
    }

    private void add(TreeReferenceLevel level) {
        hashCode = -1;
        size = -1;
        data.addElement(level);
    }

    public void add(String name, int mult) {
        add(new TreeReferenceLevel(name, mult).intern());
    }

    /**
     * Store a copy of the reference level at level 'key'.
     *
     * @param key reference level at which to attach predicate vector argument.
     * @param xpe vector of xpath expressions representing predicates to attach
     *            to a reference level.
     */
    public void addPredicate(int key, Vector<XPathExpression> xpe) {
        hashCode = -1;
        data.setElementAt(data.elementAt(key).setPredicates(xpe), key);
    }

    /**
     * Get the predicates for the reference level at level 'key'.
     *
     * @param key reference level at which to grab the predicates.
     * @return the predicates for the specified reference level.
     */
    public Vector<XPathExpression> getPredicate(int key) {
        return data.elementAt(key).getPredicates();
    }

    /**
     * @return Do any of the reference levels have predicates attached to them?
     */
    public boolean hasPredicates() {
        for (TreeReferenceLevel level : data) {
            if (level.getPredicates() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a copy of this object without any predicates attached to its
     * reference levels.
     *
     * @return a copy of this tree reference without any predicates
     */
    public TreeReference removePredicates() {
        TreeReference predicateless = cloneWithEmptyData();
        for (TreeReferenceLevel referenceLevel : data) {
            predicateless.add(referenceLevel.setPredicates(null));
        }
        return predicateless;
    }

    public int getRefLevel() {
        return refLevel;
    }

    public void setRefLevel(int refLevel) {
        hashCode = -1;
        this.refLevel = refLevel;
    }

    public void incrementRefLevel() {
        hashCode = -1;
        if (!isAbsolute()) {
            refLevel++;
        }
    }

    public boolean isAbsolute() {
        return refLevel == REF_ABSOLUTE;
    }

    /**
     * Return a copy of the reference
     */
    @Override
    public TreeReference clone() {
        TreeReference newRef = cloneWithEmptyData();

        for (TreeReferenceLevel l : data) {
            newRef.add(l.shallowCopy());
        }

        return newRef;
    }

    /**
     * Return a copy of the TreeReference that doesn't include any of the
     * TreeReferenceLevels. Useful when we are just going to overwrite the
     * levels with new data anyways.
     *
     * @return a clone of this object that doesn't include any reference level
     * data.
     */
    private TreeReference cloneWithEmptyData() {
        return new TreeReference(instanceName, refLevel, contextType);
    }

    /*
     * Chop the lowest level off the ref so that the ref now represents the
     * parent of the original ref. Return true if we successfully got the
     * parent, false if there were no higher levels
     */
    private boolean removeLastLevel() {
        int oldSize = size();
        hashCode = -1;
        this.size = -1;
        if (oldSize == 0) {
            if (isAbsolute()) {
                return false;
            } else {
                refLevel++;
                return true;
            }
        } else {
            data.removeElementAt(oldSize - 1);
            return true;
        }
    }

    public TreeReference getParentRef() {
        //TODO: level
        TreeReference ref = this.clone();
        if (ref.removeLastLevel()) {
            return ref;
        } else {
            return null;
        }
    }

    /**
     * Join this reference with the base reference argument.
     *
     * @param baseRef an absolute reference or a relative reference with only
     *                '../'s
     * @return a join of this reference with the base reference argument.
     * Returns a clone of this reference if it is absolute, and null if this
     * reference has '../'s but baseRef argument a non-empty relative reference.
     */
    public TreeReference parent(TreeReference baseRef) {
        if (isAbsolute()) {
            return this.clone();
        } else {
            TreeReference newRef = baseRef.clone();
            if (refLevel > 0) {
                if (!baseRef.isAbsolute() && baseRef.size() == 0) {
                    // if parent ref is relative and doesn't have any levels,
                    // aggregate '../' count
                    newRef.refLevel += refLevel;
                } else {
                    return null;
                }
            }

            // copy reference levels over to parent ref
            for (TreeReferenceLevel l : this.data) {
                newRef.add(l.shallowCopy());
            }

            return newRef;
        }
    }

    /**
     * Evaluate this reference in terms of a base absolute reference.
     *
     * For instance, anchoring ../../d/e/f to /a/b/c, results in  /a/d/e/f.
     *
     * NOTE: This function works when baseRef contains INDEX_UNBOUND
     * multiplicites. Conditions depend on this behavior, but it is def
     * slightly icky
     *
     * @param baseRef an absolute reference to be anchored to.
     * @return null if base reference isn't absolute or there are too many
     * '../'.
     */
    public TreeReference anchor(TreeReference baseRef) {
        // TODO: Technically we should possibly be modifying context stuff here
        // instead of in the xpath stuff;

        if (isAbsolute()) {
            return this.clone();
        } else if (!baseRef.isAbsolute() ||
                (refLevel > baseRef.size())) {
            // non-absolute anchor ref or this reference has to many '../' for
            // the anchor ref
            return null;
        } else {
            TreeReference newRef = baseRef.clone();
            // remove a level from anchor ref for each '../'
            for (int i = 0; i < refLevel; i++) {
                newRef.removeLastLevel();
            }
            // copy level data from this ref to the anchor ref
            for (int i = 0; i < size(); i++) {
                newRef.add(this.data.elementAt(i).shallowCopy());
            }
            return newRef;
        }
    }

    /**
     * Evaluate this reference in terms of the base reference argument. If this
     * reference can be made more specific by filters or predicates in the
     * context reference, it does so, but never overwrites existing filters or
     * predicates.
     *
     * @param contextRef the absolute reference used as the base while evaluating
     *                   this reference.
     * @return null if context reference is relative, a clone of this reference
     * if it is absolute and doesn't match the context reference argument.
     */
    public TreeReference contextualize(TreeReference contextRef) {
        //TODO: Technically we should possibly be modifying context stuff here
        //instead of in the xpath stuff;

        if (!contextRef.isAbsolute()) {
            return null;
        }

        // With absolute node we should know what our instance is, so no
        // further contextualizaiton can be applied unless the instances match
        if (this.isAbsolute()) {
            if (this.getInstanceName() == null) {
                // If this refers to the main instance, but our context ref
                // doesn't
                if (contextRef.getInstanceName() != null) {
                    return this.clone();
                }
            } else if (!this.getInstanceName().equals(contextRef.getInstanceName())) {
                // Or if this refers to another instance and the context ref
                // doesn't refer to the same instance
                return this.clone();
            }
        }

        TreeReference newRef = anchor(contextRef);
        if(newRef == null) {
            throw new RuntimeException("Unable to contextualize the reference |" + this.toString() +
                    "| against the provided context |" + contextRef + "|");
        }
        newRef.hashCode = -1;
        newRef.contextType = contextRef.getContext();

        // apply multiplicites and fill in wildcards as necessary, based on the
        // context ref
        for (int i = 0; i < contextRef.size() && i < newRef.size(); i++) {
            // If the the contextRef can provide a definition for a wildcard, do so
            if (TreeReference.NAME_WILDCARD.equals(newRef.getName(i)) &&
                    !TreeReference.NAME_WILDCARD.equals(contextRef.getName(i))) {
                newRef.data.setElementAt(newRef.data.elementAt(i).setName(contextRef.getName(i)), i);
            }

            if (contextRef.getName(i).equals(newRef.getName(i))) {
                // Only copy over multiplicity info if it won't overwrite any
                // existing preds or filters

                // don't copy multiplicity from context when new ref's
                // multiplicity is already bound or when the context's
                // multiplicity is not a position (but rather an attr or
                // template)
                if (newRef.getPredicate(i) == null &&
                        newRef.getMultiplicity(i) == INDEX_UNBOUND &&
                        contextRef.getMultiplicity(i) >= 0) {
                    newRef.setMultiplicity(i, contextRef.getMultiplicity(i));
                }
            } else {
                break;
            }
        }

        return newRef;
    }

    public TreeReference relativize(TreeReference parent) {
        if (parent.isParentOf(this, false)) {
            TreeReference relRef = selfRef();
            for (int i = parent.size(); i < this.size(); i++) {
                relRef.add(this.getName(i), INDEX_UNBOUND);
            }
            return relRef;
        } else {
            return null;
        }
    }

    /**
     * Turn an un-ambiguous reference into a generic one. This is acheived by
     * setting the multiplicity of every reference level to unbounded.
     *
     * @return a clone of this reference with every reference level's
     * multiplicity set to unbounded.
     */
    public TreeReference genericize() {
        return genericizeAfter(0);
    }

    public TreeReference genericizeAfter(int levelToStartGenericizing) {
        TreeReference genericRef = clone();
        for (int i = levelToStartGenericizing; i < genericRef.size(); i++) {
            // TODO: It's not super clear whether template refs should get
            // genericized or not
            if (genericRef.getMultiplicity(i) > -1 ||
                    genericRef.getMultiplicity(i) == INDEX_TEMPLATE) {
                genericRef.setMultiplicity(i, INDEX_UNBOUND);
            }
        }
        return genericRef;
    }

    /**
     * Are these reference's levels subsumed by equivalently named 'child'
     * levels of the same multiplicity?
     *
     * @param child        check if this reference is a child of the current reference
     * @param properParent when set don't return true if 'child' is equal to
     *                     this
     * @return true if 'this' is parent of 'child' or if 'this' equals 'child'
     * (when properParent is false)
     */
    public boolean isParentOf(TreeReference child, boolean properParent) {
        if ((refLevel != child.refLevel) ||
                (child.size() < (size() + (properParent ? 1 : 0)))) {
            return false;
        }

        for (int i = 0; i < size(); i++) {
            // check that levels names are the same
            if (!this.getName(i).equals(child.getName(i))) {
                return false;
            }

            // check that multiplicities are the same; allowing them to differ
            // if on 0-th level, parent mult is the default and child is
            // unbounded.
            int parMult = this.getMultiplicity(i);
            int childMult = child.getMultiplicity(i);
            if (parMult != INDEX_UNBOUND &&
                    parMult != childMult &&
                    !(i == 0 && parMult == 0 && childMult == INDEX_UNBOUND)) {
                return false;
            }
        }

        return true;
    }

    /**
     * clone and extend a reference by one level
     */
    public TreeReference extendRef(String name, int mult) {
        //TODO: Shouldn't work for this if this is an attribute ref;
        TreeReference childRef = this.clone();
        childRef.add(name, mult);
        return childRef;
    }

    /**
     * Equality of two TreeReferences comes down to having the same reference
     * level, and equal reference levels entries.
     *
     * @param o an object to compare against this TreeReference object.
     * @return Is object o a TreeReference with equal reference level entries
     * to this object?
     */
    @Override
    public boolean equals(Object o) {
        //csims@dimagi.com - Replaced this function performing itself fully written out
        //rather than allowing the tree reference levels to denote equality. The only edge
        //case was identifying that /data and /data[0] were always the same. I don't think
        //that should matter, but noting in case there are issues in the future.
        if (this == o) {
            return true;
        } else if (o instanceof TreeReference) {
            TreeReference ref = (TreeReference)o;

            if (this.refLevel == ref.refLevel && this.size() == ref.size()) {
                // loop through reference segments, comparing their equality
                for (int i = 0; i < this.size(); i++) {
                    TreeReferenceLevel thisLevel = data.elementAt(i);
                    TreeReferenceLevel otherLevel = ref.data.elementAt(i);

                    if (!thisLevel.equals(otherLevel)) {
                        return false;
                    }
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode != -1) {
            return hashCode;
        }
        int hash = refLevel;
        for (int i = 0; i < size(); i++) {
            int mult = getMultiplicity(i);
            if (i == 0 && mult == INDEX_UNBOUND) {
                mult = 0;
            }

            hash ^= getName(i).hashCode();
            hash ^= mult;

            Vector<XPathExpression> predicates = this.getPredicate(i);
            if (predicates != null) {
                int val = 0;
                for (XPathExpression xpe : predicates) {
                    hash ^= val;
                    hash ^= xpe.hashCode();
                    ++val;
                }
            }
        }
        hashCode = hash;
        return hash;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean includePredicates) {
        StringBuffer sb = new StringBuffer();
        if (instanceName != null) {
            sb.append("instance(").append(instanceName).append(")");
        } else if (contextType == CONTEXT_ORIGINAL) {
            sb.append("current()/");
        }
        if (isAbsolute()) {
            sb.append("/");
        } else {
            for (int i = 0; i < refLevel; i++)
                sb.append("../");
        }
        for (int i = 0; i < size(); i++) {
            String name = getName(i);
            int mult = getMultiplicity(i);

            if (mult == INDEX_ATTRIBUTE) {
                sb.append("@");
            }
            sb.append(name);

            if (includePredicates) {
                switch (mult) {
                    case INDEX_UNBOUND:
                        Vector<XPathExpression> predicates = this.getPredicate(i);
                        if (predicates != null) {
                            for (XPathExpression expr : predicates) {
                                sb.append("[").append(expr.toPrettyString()).append("]");
                            }
                        }
                        break;
                    case INDEX_TEMPLATE:
                        sb.append("[@template]");
                        break;
                    case INDEX_REPEAT_JUNCTURE:
                        sb.append("[@juncture]");
                        break;
                    default:
                        // Don't show a multiplicity selector if we are
                        // selecting the 1st element, since this is the default
                        // and showing brackets might confuse the user.
                        if ((i > 0 || mult != 0) && mult != -4) {
                            sb.append("[").append(mult + 1).append("]");
                        }
                        break;
                }
            }

            if (i < size() - 1) {
                sb.append("/");
            }
        }
        return sb.toString();
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        refLevel = ExtUtil.readInt(in);
        instanceName = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        contextType = ExtUtil.readInt(in);
        int size = ExtUtil.readInt(in);
        for (int i = 0; i < size; ++i) {
            TreeReferenceLevel level = (TreeReferenceLevel)ExtUtil.read(in, TreeReferenceLevel.class, pf);
            this.add(level.intern());
        }
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, refLevel);
        ExtUtil.write(out, new ExtWrapNullable(instanceName));
        ExtUtil.writeNumeric(out, contextType);
        ExtUtil.writeNumeric(out, size());
        for (TreeReferenceLevel l : data) {
            ExtUtil.write(out, l);
        }
    }

    /**
     * Intersect this tree reference with another, returning a new tree reference
     * which contains all of the common elements, starting with the root element.
     *
     * Note that relative references by their nature can't share steps, so intersecting
     * any (or by any) relative ref will result in the root ref. Additionally, if the
     * two references don't share any steps, the intersection will consist of the root
     * reference.
     *
     * @param b The tree reference to intersect
     * @return The tree reference containing the common basis of this ref and b
     */
    public TreeReference intersect(TreeReference b) {
        if (!this.isAbsolute() || !b.isAbsolute()) {
            return TreeReference.rootRef();
        }
        if (this.equals(b)) {
            return this;
        }

        TreeReference a;
        //A should always be bigger if one ref is larger than the other
        if (this.size() < b.size()) {
            a = b.clone();
            b = this.clone();
        } else {
            a = this.clone();
            b = b.clone();
        }

        //Now, trim the refs to the same length.
        int diff = a.size() - b.size();
        for (int i = 0; i < diff; ++i) {
            a.removeLastLevel();
        }

        int aSize = a.size();
        //easy, but requires a lot of re-evaluation.
        for (int i = 0; i <= aSize; ++i) {
            if (a.equals(b)) {
                return a;
            } else if (a.size() == 0) {
                return TreeReference.rootRef();
            } else {
                if (!a.removeLastLevel() || !b.removeLastLevel()) {
                    //I don't think it should be possible for us to get here, so flip if we do
                    throw new RuntimeException("Dug too deply into TreeReference during intersection");
                }
            }
        }

        //The only way to get here is if a's size is -1
        throw new RuntimeException("Impossible state");
    }

    public int getContext() {
        return this.contextType;
    }

    /**
     * Returns the subreference of this reference up to the level specified.
     *
     * For instance, for the reference:
     * (/data/path/to/node).getSubreference(2) => /data/path/to
     *
     * Used to identify the reference context for a predicate at the same level
     *
     * @param level number of segments to include in the truncated
     *              sub-reference.
     * @return A clone of this reference object that includes steps up the
     * specified level.
     * @throws IllegalArgumentException if this object isn't an absolute
     *                                  reference.
     */
    public TreeReference getSubReference(int level) {
        if (!this.isAbsolute()) {
            throw new IllegalArgumentException("Cannot subreference a non-absolute ref");
        }

        TreeReference subRef = cloneWithEmptyData();
        for (int i = 0; i <= level; ++i) {
            subRef.add(this.data.elementAt(i));
        }
        return subRef;
    }
}
