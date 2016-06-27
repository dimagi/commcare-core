package org.commcare.suite.model;

import org.commcare.session.RemoteQuerySessionManager;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMapPoly;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
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
    private XPathExpression relevantExpr;
    private Hashtable<String, XPathExpression> params;

    @SuppressWarnings("unused")
    public SyncPost() {
    }

    public SyncPost(String url, XPathExpression relevantExpr,
                    Hashtable<String, XPathExpression> params) {
        this.url = (url == null) ? "" : url;
        this.params = params;
        this.relevantExpr = relevantExpr;
    }

    public String getUrl() {
        return url;
    }

    public Hashtable<String, String> getEvaluatedParams(EvaluationContext evalContext) {
        Hashtable<String, String> evaluatedParams = new Hashtable<>();
        for(Enumeration en = params.keys(); en.hasMoreElements(); ) {
            String key = (String)en.nextElement();
            evaluatedParams.put(key,
                    RemoteQuerySessionManager.evalXpathExpression(params.get(key), evalContext));
        }
        return evaluatedParams;
    }

    public boolean isRelevant(EvaluationContext evalContext) {
        if (relevantExpr == null) {
            return true;
        } else {
            String result = RemoteQuerySessionManager.evalXpathExpression(relevantExpr, evalContext);
            return "true".equals(result);
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        params = (Hashtable<String, XPathExpression>)ExtUtil.read(in, new ExtWrapMapPoly(String.class), pf);
        url = ExtUtil.readString(in);
        relevantExpr = (XPathExpression)ExtUtil.read(in, new ExtWrapNullable(new ExtWrapTagged()), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapMapPoly(params));
        ExtUtil.writeString(out, url);
        ExtUtil.write(out, new ExtWrapNullable(relevantExpr == null ? null : new ExtWrapTagged(relevantExpr)));
    }
}
