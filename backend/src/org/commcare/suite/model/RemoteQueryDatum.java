package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapMapPoly;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

/**
 * Entry config for querying a remote server with user and session provided
 * parameters and storing the xml data response in an instance.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class RemoteQueryDatum extends SessionDatum {
    private Hashtable hiddenQueryValues;
    private Hashtable<String, DisplayUnit> userQueryPrompts;

    @SuppressWarnings("unused")
    public RemoteQueryDatum() {
    }

    public RemoteQueryDatum(String url, String storageInstance,
                            Hashtable<String, XPathExpression> hiddenQueryValues,
                            Hashtable<String, DisplayUnit> userQueryPrompts) {
        super(storageInstance, url);
        this.hiddenQueryValues = hiddenQueryValues;
        this.userQueryPrompts = userQueryPrompts;
    }

    public Hashtable<String, DisplayUnit> getUserQueryPrompts() {
        return userQueryPrompts;
    }

    public Hashtable<String, XPathExpression> getHiddenQueryValues() {
        return hiddenQueryValues;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);

        hiddenQueryValues =
                (Hashtable) ExtUtil.read(in, new ExtWrapMapPoly(String.class));
        userQueryPrompts =
                (Hashtable<String, DisplayUnit>) ExtUtil.read(in,
                        new ExtWrapMap(String.class, DisplayUnit.class));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);

        ExtUtil.write(out, new ExtWrapMapPoly(hiddenQueryValues));
        ExtUtil.write(out, new ExtWrapMap(userQueryPrompts));
    }
}
