package org.javarosa.model.xform;

import org.javarosa.core.model.condition.HashRefResolver;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class XPathReference implements Externalizable {
    private TreeReference ref;
    private String nodeset;

    public XPathReference() {
        // for externalization
    }

    public XPathReference(String nodeset, HashRefResolver hashRefResolver) {
        ref = getPathExpr(nodeset, hashRefResolver).getReference();
        this.nodeset = nodeset;
    }

    public XPathReference(String nodeset) {
        ref = getPathExpr(nodeset).getReference();
        this.nodeset = nodeset;
    }

    public static XPathPathExpr getPathExpr(String nodeset) {
        return getPathExpr(nodeset, null);
    }

    public static XPathPathExpr getPathExpr(String nodeset, HashRefResolver hashRefResolver) {
        XPathExpression path;
        boolean validNonPathExpr = false;

        try {
            path = XPathParseTool.parseXPath(nodeset, hashRefResolver);
            if (!(path instanceof XPathPathExpr)) {
                validNonPathExpr = true;
                throw new XPathSyntaxException();
            }

        } catch (XPathSyntaxException xse) {
            //make these checked exceptions?
            if (validNonPathExpr) {
                throw new XPathTypeMismatchException("Expected XPath path, got XPath expression: [" + nodeset + "]," + xse.getMessage());
            } else {
                xse.printStackTrace();
                throw new XPathException("Parse error in XPath path: [" + nodeset + "]." + (xse.getMessage() == null ? "" : "\n" + xse.getMessage()));
            }
        }

        return (XPathPathExpr)path;
    }

    public XPathReference(XPathPathExpr path) {
        ref = path.getReference();
    }

    public XPathReference(TreeReference ref) {
        this.ref = ref;
    }

    public TreeReference getReference() {
        return ref;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof XPathReference &&
                ref.equals(((XPathReference)o).ref);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        nodeset = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        ref = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(nodeset));
        ExtUtil.write(out, ref);
    }
}
