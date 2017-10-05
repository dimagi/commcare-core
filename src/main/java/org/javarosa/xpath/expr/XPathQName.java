package org.javarosa.xpath.expr;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * An XPathQName is string literal that meets the requirements to be an element or attribute
 * name in an XML document
 */
public class XPathQName implements Externalizable {
    private String namespace;
    public String name;
    private int hashCode;

    public XPathQName() {
    } //for deserialization

    public XPathQName(String qname) {
        int sep = (qname == null ? -1 : qname.indexOf(":"));
        if (sep == -1) {
            init(null, qname);
        } else {
            init(qname.substring(0, sep), qname.substring(sep + 1));
        }
    }

    public XPathQName(String namespace, String name) {
        init(namespace, name);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private void init(String namespace, String name) {
        if (name == null
                || name.length() == 0
                || (namespace != null && namespace.length() == 0))
            throw new IllegalArgumentException("Invalid QName");

        this.namespace = namespace;
        this.name = name;
        cacheCode();
    }

    private void cacheCode() {
        hashCode = name.hashCode() ^ (namespace == null ? 0 : namespace.hashCode());
    }

    @Override
    public String toString() {
        return (namespace == null ? name : namespace + ":" + name);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XPathQName) {
            XPathQName x = (XPathQName)o;
            if (hashCode != o.hashCode()) {
                return false;
            }
            return ExtUtil.equals(namespace, x.namespace, false) && name.equals(x.name);
        } else {
            return false;
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        namespace = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        name = ExtUtil.readString(in);
        cacheCode();
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapNullable(namespace));
        ExtUtil.writeString(out, name);
    }
}
