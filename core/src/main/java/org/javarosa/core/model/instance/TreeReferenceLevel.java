/**
 *
 */
package org.javarosa.core.model.instance;

import org.javarosa.core.util.ArrayUtilities;
import org.javarosa.core.util.Interner;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * @author ctsims
 */
public class TreeReferenceLevel implements Externalizable {
    public static final int MULT_UNINIT = -16;

    private String name;
    private int multiplicity = MULT_UNINIT;
    private Vector<XPathExpression> predicates;

    // a cache for refence levels, to avoid keeping a bunch of the same levels
    // floating around at run-time.
    private static Interner<TreeReferenceLevel> refs;

    // Do we want to keep a cache of all reference levels?
    public static boolean treeRefLevelInterningEnabled = true;

    public static void attachCacheTable(Interner<TreeReferenceLevel> refs) {
        TreeReferenceLevel.refs = refs;
    }

    public TreeReferenceLevel() {
    }


    public TreeReferenceLevel(String name, int multiplicity, Vector<XPathExpression> predicates) {
        this.name = name;
        this.multiplicity = multiplicity;
        this.predicates = predicates;
    }

    public TreeReferenceLevel(String name, int multiplicity) {
        this(name, multiplicity, null);
    }


    public int getMultiplicity() {
        return multiplicity;
    }

    public String getName() {
        return name;
    }

    public TreeReferenceLevel setMultiplicity(int mult) {
        return new TreeReferenceLevel(name, mult, predicates).intern();
    }

    /**
     * Create a copy of this level with updated predicates.
     *
     * @param xpe vector of xpath expressions representing predicates to attach
     *            to a copy of this reference level.
     * @return a (cached-)copy of this reference level with the predicates argument
     * attached.
     */
    public TreeReferenceLevel setPredicates(Vector<XPathExpression> xpe) {
        return new TreeReferenceLevel(name, multiplicity, xpe).intern();
    }

    public Vector<XPathExpression> getPredicates() {
        return this.predicates;
    }

    public TreeReferenceLevel shallowCopy() {
        return new TreeReferenceLevel(name, multiplicity,
                ArrayUtilities.vectorCopy(predicates)).intern();
    }


    public TreeReferenceLevel setName(String name) {
        return new TreeReferenceLevel(name, multiplicity, predicates).intern();
    }


    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        name = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        multiplicity = ExtUtil.readInt(in);
        predicates = ExtUtil.nullIfEmpty((Vector<XPathExpression>)ExtUtil.read(in, new ExtWrapListPoly()));
    }


    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(name));
        ExtUtil.writeNumeric(out, multiplicity);
        ExtUtil.write(out, new ExtWrapListPoly(ExtUtil.emptyIfNull(predicates)));
    }

    public int hashCode() {
        int predPart = 0;
        if (predicates != null) {
            for (XPathExpression xpe : predicates) {
                predPart ^= xpe.hashCode();
            }
        }

        return name.hashCode() ^ multiplicity ^ predPart;
    }

    /**
     * Two TreeReferenceLevels are equal if they have the same name,
     * multiplicity, and equal predicates.
     *
     * @param o an object to compare against this TreeReferenceLevel object.
     * @return Is object o a TreeReferenceLevel and has the same fields?
     */
    public boolean equals(Object o) {
        if (!(o instanceof TreeReferenceLevel)) {
            return false;
        }

        TreeReferenceLevel l = (TreeReferenceLevel)o;
        // multiplicity and names match-up
        if ((multiplicity != l.multiplicity) ||
                (name == null && l.name != null) ||
                (!name.equals(l.name))) {
            return false;
        }

        if (predicates == null && l.predicates == null) {
            return true;
        }

        // predicates match-up
        if ((predicates == null && l.predicates != null) ||
                (l.predicates == null && predicates != null) ||
                (predicates.size() != l.predicates.size())) {
            return false;
        }

        // predicate elements are equal
        for (int i = 0; i < predicates.size(); ++i) {
            if (!predicates.elementAt(i).equals(l.predicates.elementAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Make sure this object has been added to the cache table.
     */
    public TreeReferenceLevel intern() {
        if (!treeRefLevelInterningEnabled || refs == null) {
            return this;
        } else {
            return refs.intern(this);
        }
    }
}
