package org.javarosa.xpath;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.text.MessageFormat;
import java.util.Vector;

/**
 * Represents a set of XPath nodes returned from a path or other operation which acts on multiple
 * paths.
 *
 * Current encompasses two states.
 *
 * 1) A nodeset which references between 0 and N nodes which are known about (but, for instance,
 * don't match any predicates or are irrelevant). Some operations cannot be evaluated in this state
 * directly. If more than one node is referenced, it is impossible to return a normal evaluation, for
 * instance.
 *
 * 2) A nodeset which wasn't able to reference into any known model (generally a reference which is
 * written in error). In this state, the size of the nodeset can be evaluated, but the acual reference
 * cannot be returned, since it doesn't have any semantic value.
 *
 * (2) may be a deviation from normal XPath. This should be evaluated in the future.
 *
 * @author ctsims
 */
public class XPathNodeset {

    private Vector<TreeReference> nodes;
    protected DataInstance instance;
    protected EvaluationContext ec;
    private String pathEvaluated;
    private String originalPath;

    private XPathNodeset() {

    }

    /**
     * for lazy evaluation
     */
    protected XPathNodeset(DataInstance instance, EvaluationContext ec) {
        this.instance = instance;
        this.ec = ec;
    }

    public static XPathNodeset constructInvalidPathNodeset(String pathEvaluated, String originalPath) {
        XPathNodeset nodeset = new XPathNodeset();
        nodeset.nodes = null;
        nodeset.instance = null;
        nodeset.ec = null;
        nodeset.pathEvaluated = pathEvaluated;
        nodeset.originalPath = originalPath;
        return nodeset;
    }

    protected void setReferences(Vector<TreeReference> nodes) {
        this.nodes = nodes;
    }

    public Vector<TreeReference> getReferences() {
        return this.nodes;
    }

    /**
     * @return The value represented by this xpath. Can only be evaluated when this xpath represents exactly one
     * reference, or when it represents 0 references after a filtering operation (a reference which _could_ have
     * existed, but didn't, rather than a reference which could not represent a real node).
     */
    public Object unpack() {
        if (nodes == null) {
            throw getInvalidNodesetException();
        }

        if (size() == 0) {
            return XPathPathExpr.unpackValue(null);
        } else if (size() > 1) {
            throw new XPathTypeMismatchException("XPath nodeset has more than one node [" + nodeContents() + "]; cannot convert multiple nodes to a raw value. Refine path expression to match only one node.");
        } else {
            return getValAt(0);
        }
    }

    public Object[] toArgList() {
        if (nodes == null) {
            throw getInvalidNodesetException();
        }

        Object[] args = new Object[size()];

        for (int i = 0; i < size(); i++) {
            Object val = getValAt(i);

            //sanity check
            if (val == null) {
                throw new RuntimeException("retrived a null value out of a nodeset! shouldn't happen!");
            }

            args[i] = val;
        }

        return args;
    }

    public int size() {
        if (nodes == null) {
            return 0;
        }
        return nodes.size();
    }

    public TreeReference getRefAt(int i) {
        if (nodes == null) {
            throw getInvalidNodesetException();
        }

        return nodes.elementAt(i);
    }

    public DataInstance getInstance() {
        return instance;
    }

    protected Object getValAt(int i) {
        return XPathPathExpr.getRefValue(instance, ec, getRefAt(i));
    }

    protected XPathTypeMismatchException getInvalidNodesetException() {
        if (!pathEvaluated.equals(originalPath)) {
            if (!originalPath.contains("/data")) {
                throw new XPathTypeMismatchException(MessageFormat.format(
                        "Logic references {0} which is not a valid question or value." +
                        " You may have forgotten to include the full path to the question " +
                        "(e.g. /data/{0}). (Expanded reference: {1})", originalPath, pathEvaluated));
            } else {
                throw new XPathTypeMismatchException(MessageFormat.format(
                        "There was a problem with the path {0}" +
                        " which refers to location which was not found ({1}). " +
                        "This often means you made a typo in the question reference, " +
                        "or the question no longer exists in the form.", originalPath, pathEvaluated));
            }
        } else {
            throw new XPathTypeMismatchException(MessageFormat.format(
                    "Logic references {0} which is not a valid question or value.", pathEvaluated));
        }
    }
    
    protected String nodeContents() {
        if (nodes == null) {
            return "Invalid Path: " + pathEvaluated;
        }
        return printNodeContents(nodes);
    }

    public static String printNodeContents(Vector<TreeReference> nodes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < nodes.size(); i++) {
            sb.append(nodes.elementAt(i).toString());
            if (i < nodes.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }
}
