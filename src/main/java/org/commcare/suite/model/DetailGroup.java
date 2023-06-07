package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DetailGroup implements Externalizable {

    private XPathExpression function;
    private Integer headerRows;

    /**
     * Serialization only!!!
     */
    @SuppressWarnings("unused")
    public DetailGroup() {
    }

    public DetailGroup(XPathExpression function, Integer headerRows) {
        this.function = function;
        this.headerRows = headerRows;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        function = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
        headerRows = ExtUtil.readInt(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(function));
        ExtUtil.write(out, headerRows);
    }

    public XPathExpression getFunction() {
        return function;
    }

    public Integer getHeaderRows() {
        return headerRows;
    }
}
