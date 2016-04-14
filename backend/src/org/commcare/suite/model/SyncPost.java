package org.commcare.suite.model;

import org.commcare.session.RemoteQuerySessionManager;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Entry config for posting data to a remote server as part of synchronous
 * request transaction.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class SyncPost implements Externalizable {
    private String url;
    private Hashtable<String, XPathExpression> params;

    @SuppressWarnings("unused")
    public SyncPost() {
    }

    public SyncPost(String url, Hashtable<String, XPathExpression> params) {
        this.url = (url == null) ? "" : url;
        this.params = params;
    }

    public String getUrl() {
        return url;
    }

    public Hashtable<String, String> getEvaluatedParams(EvaluationContext evaluationContext) {
        Hashtable<String, String> evaluatedParams = new Hashtable<String, String>();
        for(Enumeration en = params.keys(); en.hasMoreElements(); ) {
            String key = (String)en.nextElement();
            evaluatedParams.put(key,
                    RemoteQuerySessionManager.calculateHidden(params.get(key), evaluationContext));
        }
        return evaluatedParams;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        params = (Hashtable<String, XPathExpression>)ExtUtil.read(in, new ExtWrapMap(String.class, XPathExpression.class));
        url = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapMap(params));
        ExtUtil.writeString(out, url);
    }
}
