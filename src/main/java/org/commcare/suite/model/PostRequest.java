package org.commcare.suite.model;

import org.commcare.session.RemoteQuerySessionManager;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;

/**
 * Entry config for posting data to a remote server as part of synchronous
 * request transaction.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class PostRequest implements Externalizable {
    private URL url;
    private XPathExpression relevantExpr;
    private List<QueryData> params;

    @SuppressWarnings("unused")
    public PostRequest() {
    }

    public PostRequest(URL url, XPathExpression relevantExpr, List<QueryData> params) {
        this.url = url;
        this.params = params;
        this.relevantExpr = relevantExpr;
    }

    public URL getUrl() {
        return url;
    }

    public Hashtable<String, String> getEvaluatedParams(EvaluationContext evalContext) {
        Hashtable<String, String> evaluatedParams = new Hashtable<>();
        for (QueryData param : params) {
            evaluatedParams.put(param.getKey(), param.getValue(evalContext));
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
        params = (List<QueryData>) ExtUtil.read(in, new ExtWrapList(new ExtWrapTagged()), pf);
        url = new URL(ExtUtil.readString(in));
        relevantExpr = (XPathExpression)ExtUtil.read(in, new ExtWrapNullable(new ExtWrapTagged()), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapList(params, new ExtWrapTagged()));
        ExtUtil.writeString(out, url.toString());
        ExtUtil.write(out, new ExtWrapNullable(relevantExpr == null ? null : new ExtWrapTagged(relevantExpr)));
    }
}
