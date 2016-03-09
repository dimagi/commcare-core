package org.javarosa.core.model;

import org.javarosa.core.model.instance.TreeReference;

import java.util.Vector;

/**
 * A Form Index is an immutable index into a specific question definition that
 * will appear in an interaction with a user.
 *
 * An index is represented by different levels into hierarchical groups.
 *
 * Indices can represent both questions and groups.
 *
 * It is absolutely essential that there be no circularity of reference in
 * FormIndex's, IE, no form index's ancestor can be itself.
 *
 * Datatype Productions:
 * FormIndex = BOF | EOF | CompoundIndex(nextIndex:FormIndex,Location)
 * Location = Empty | Simple(localLevel:int) | WithMult(localLevel:int, multiplicity:int)
 *
 * @author Clayton Sims
 */
public class FormIndex {

    private boolean beginningOfForm = false;
    private boolean endOfForm = false;

    /**
     * The index of the questiondef in the current context
     */
    private final int localIndex;

    /**
     * The multiplicity of the current instance of a repeated question or group
     */
    private int instanceIndex = -1;

    /**
     * The next level of this index
     */
    private FormIndex nextLevel;

    private TreeReference reference;

    public static FormIndex createBeginningOfFormIndex() {
        FormIndex begin = new FormIndex(-1, null);
        begin.beginningOfForm = true;
        return begin;
    }

    public static FormIndex createEndOfFormIndex() {
        FormIndex end = new FormIndex(-1, null);
        end.endOfForm = true;
        return end;
    }

    /**
     * Constructs a simple form index that references a specific element in
     * a list of elements.
     *
     * @param localIndex An integer index into a flat list of elements
     * @param reference  A reference to the instance element identified by this index;
     */
    public FormIndex(int localIndex, TreeReference reference) {
        this.localIndex = localIndex;
        this.reference = reference;

    }

    /**
     * Constructs a simple form index that references a specific element in
     * a list of elements.
     *
     * @param localIndex    An integer index into a flat list of elements
     * @param instanceIndex An integer index expressing the multiplicity
     *                      of the current level
     * @param reference     A reference to the instance element identified by this index;
     */
    public FormIndex(int localIndex, int instanceIndex, TreeReference reference) {
        this.localIndex = localIndex;
        this.instanceIndex = instanceIndex;
        this.reference = reference;
    }

    /**
     * Constructs an index which indexes an element, and provides an index
     * into that elements children
     *
     * @param nextLevel  An index into the referenced element's index
     * @param localIndex An index to an element at the current level, a child
     *                   element of which will be referenced by the nextLevel index.
     * @param reference  A reference to the instance element identified by this index;
     */
    public FormIndex(FormIndex nextLevel, int localIndex, TreeReference reference) {
        this(localIndex, reference);
        this.nextLevel = nextLevel;
    }

    /**
     * Constructs an index which references an element past the level of
     * specificity of the current context, founded by the currentLevel
     * index.
     * (currentLevel, (nextLevel...))
     */
    public FormIndex(FormIndex nextLevel, FormIndex currentLevel) {
        if (currentLevel == null) {
            this.nextLevel = nextLevel.nextLevel;
            this.localIndex = nextLevel.localIndex;
            this.instanceIndex = nextLevel.instanceIndex;
            this.reference = nextLevel.reference;
        } else {
            this.nextLevel = nextLevel;
            this.localIndex = currentLevel.getLocalIndex();
            this.instanceIndex = currentLevel.getInstanceIndex();
            this.reference = currentLevel.reference;
        }
    }

    /**
     * Constructs an index which indexes an element, and provides an index
     * into that elements children, along with the current index of a
     * repeated instance.
     *
     * @param nextLevel     An index into the referenced element's index
     * @param localIndex    An index to an element at the current level, a child
     *                      element of which will be referenced by the nextLevel index.
     * @param instanceIndex How many times the element referenced has been
     *                      repeated.
     * @param reference     A reference to the instance element identified by this index;
     */
    public FormIndex(FormIndex nextLevel, int localIndex, int instanceIndex, TreeReference reference) {
        this(nextLevel, localIndex, reference);
        this.instanceIndex = instanceIndex;
    }

    public boolean isInForm() {
        return !beginningOfForm && !endOfForm;
    }

    /**
     * @return The index of the element in the current context
     */
    public int getLocalIndex() {
        return localIndex;
    }

    /**
     * @return The multiplicity of the current instance of a repeated question or group
     */
    public int getInstanceIndex() {
        return instanceIndex;
    }

    /**
     * For the fully qualified element, get the multiplicity of the element's reference
     *
     * @return The terminal element (fully qualified)'s instance index
     */
    public int getElementMultiplicity() {
        return getTerminal().instanceIndex;
    }

    /**
     * @return An index into the next level of specificity past the current context. An
     * example would be an index  into an element that is a child of the element referenced
     * by the local index.
     */
    public FormIndex getNextLevel() {
        return nextLevel;
    }

    public TreeReference getLocalReference() {
        return reference;
    }

    /**
     * @return The TreeReference of the fully qualified element described by this
     * FormIndex.
     */
    public TreeReference getReference() {
        return getTerminal().reference;
    }

    public FormIndex getTerminal() {
        FormIndex walker = this;
        while (walker.nextLevel != null) {
            walker = walker.nextLevel;
        }
        return walker;
    }

    /**
     * Identifies whether this is a terminal index, in other words whether this
     * index references with more specificity than the current context
     */
    public boolean isTerminal() {
        return nextLevel == null;
    }

    public boolean isEndOfFormIndex() {
        return endOfForm;
    }

    public boolean isBeginningOfFormIndex() {
        return beginningOfForm;
    }

    @Override
    public int hashCode() {
        return (beginningOfForm ? 0 : 31)
                ^ (endOfForm ? 0 : 31)
                ^ localIndex
                ^ instanceIndex
                ^ (nextLevel == null ? 0 : nextLevel.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FormIndex))
            return false;

        FormIndex a = this;
        FormIndex b = (FormIndex)o;

        return (a.compareTo(b) == 0);
    }

    public int compareTo(Object o) {
        if (!(o instanceof FormIndex))
            throw new IllegalArgumentException("Attempt to compare Object of type " + o.getClass().getName() + " to a FormIndex");

        FormIndex a = this;
        FormIndex b = (FormIndex)o;

        if (a.beginningOfForm) {
            return (b.beginningOfForm ? 0 : -1);
        } else if (a.endOfForm) {
            return (b.endOfForm ? 0 : 1);
        } else {
            //a is in form
            if (b.beginningOfForm) {
                return 1;
            } else if (b.endOfForm) {
                return -1;
            }
        }

        if (a.localIndex != b.localIndex) {
            return (a.localIndex < b.localIndex ? -1 : 1);
        } else if (a.instanceIndex != b.instanceIndex) {
            return (a.instanceIndex < b.instanceIndex ? -1 : 1);
        } else if ((a.getNextLevel() == null) != (b.getNextLevel() == null)) {
            return (a.getNextLevel() == null ? -1 : 1);
        } else if (a.getNextLevel() != null) {
            return a.getNextLevel().compareTo(b.getNextLevel());
        } else {
            return 0;
        }
    }

    /**
     * @return Only the local component of this Form Index.
     */
    public FormIndex snip() {
        return new FormIndex(localIndex, instanceIndex, reference);
    }

    /**
     * Takes in a form index which is a subset of this index, and returns the
     * total difference between them. This is useful for stepping up the level
     * of index specificty. If the subIndex is not a valid subIndex of this index,
     * null is returned. Since the FormIndex represented by null is always a subset,
     * if null is passed in as a subIndex, the full index is returned
     *
     * For example:
     * Indices
     * a = 1_0,2,1,3
     * b = 1,3
     *
     * a.diff(b) = 1_0,2
     */
    public FormIndex diff(FormIndex subIndex) {
        if (subIndex == null) {
            return this;
        }
        if (!isSubIndex(this, subIndex)) {
            return null;
        }
        if (subIndex.equals(this)) {
            return null;
        }
        return new FormIndex(nextLevel.diff(subIndex), this.snip());
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        FormIndex ref = this;
        while (ref != null) {
            ret.append(ref.getLocalIndex())
                    .append(ref.getInstanceIndex() == -1 ? ", " : "_" + ref.getInstanceIndex() + ", ");
            ref = ref.nextLevel;
        }

        return ret.toString().substring(0, ret.lastIndexOf(","));
    }

    /**
     * @return the level of this index relative to the top level of the form
     */
    public int getDepth() {
        int depth = 0;
        FormIndex ref = this;
        while (ref != null) {
            ref = ref.nextLevel;
            depth++;
        }
        return depth;
    }

    private static boolean isSubIndex(FormIndex parent, FormIndex child) {
        return child.equals(parent) ||
                (parent != null && isSubIndex(parent.nextLevel, child));
    }

    public static boolean isSubElement(FormIndex parent, FormIndex child) {
        while (!parent.isTerminal() && !child.isTerminal()) {
            if (parent.getLocalIndex() != child.getLocalIndex()) {
                return false;
            }
            if (parent.getInstanceIndex() != child.getInstanceIndex()) {
                return false;
            }
            parent = parent.nextLevel;
            child = child.nextLevel;
        }
        //If we've gotten this far, at least one of the two is terminal
        if (!parent.isTerminal() && child.isTerminal()) {
            //can't be the parent if the child is earlier on
            return false;
        } else if (parent.getLocalIndex() != child.getLocalIndex()) {
            //Either they're at the same level, in which case only
            //identical indices should match, or they should have
            //the same root
            return false;
        } else if (parent.getInstanceIndex() != -1 && (parent.getInstanceIndex() != child.getInstanceIndex())) {
            return false;
        }
        //Barring all of these cases, it should be true.
        return true;
    }

    /**
     * @return Do all the entries of two FormIndexes match except for the last instance index?
     */
    public static boolean areSiblings(FormIndex a, FormIndex b) {
        if (a.isTerminal() && b.isTerminal() && a.getLocalIndex() == b.getLocalIndex()) {
            return true;
        }
        if (!a.isTerminal() && !b.isTerminal()) {
            return a.getLocalIndex() == b.getLocalIndex() &&
                    areSiblings(a.nextLevel, b.nextLevel);
        }

        return false;
    }

    /**
     * @return Do all the local indexes in the 'parent' FormIndex match the
     * corresponding ones in 'child'?
     */
    public static boolean overlappingLocalIndexesMatch(FormIndex parent, FormIndex child) {
        if (parent.getDepth() > child.getDepth()) {
            return false;
        }
        while (!parent.isTerminal()) {
            if (parent.getLocalIndex() != child.getLocalIndex()) {
                return false;
            }
            parent = parent.nextLevel;
            child = child.nextLevel;
        }
        return parent.getLocalIndex() == child.getLocalIndex();
    }

    /**
     * Used by Touchforms
     */
    public void assignRefs(FormDef f) {
        FormIndex cur = this;

        Vector<Integer> indexes = new Vector<Integer>();
        Vector<Integer> multiplicities = new Vector<Integer>();
        Vector<IFormElement> elements = new Vector<IFormElement>();
        f.collapseIndex(this, indexes, multiplicities, elements);

        Vector<Integer> curMults = new Vector<Integer>();
        Vector<IFormElement> curElems = new Vector<IFormElement>();

        int i = 0;
        while (cur != null) {
            curMults.addElement(multiplicities.elementAt(i));
            curElems.addElement(elements.elementAt(i));
            cur.reference = f.getChildInstanceRef(curElems, curMults);
            cur = cur.getNextLevel();
            i++;
        }
    }
}
