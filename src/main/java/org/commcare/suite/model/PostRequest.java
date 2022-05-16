package org.commcare.suite.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

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

    /**
     * Evalulates parameteres for post request
     *
     * @param evalContext        Context params needs to be evaluated in
     * @param includeBlankValues whether to include blank values in the return map
     * @return Evaluated params
     */
    public Multimap<String, String> getEvaluatedParams(EvaluationContext evalContext, boolean includeBlankValues) {
        Multimap<String, String> evaluatedParams = ArrayListMultimap.create();
        for (QueryData queryData : params) {
            Iterable<String> val = queryData.getValues(evalContext);
            if (val.iterator().hasNext()) {
                evaluatedParams.putAll(queryData.getKey(), val);
            } else if (includeBlankValues) {
                evaluatedParams.put(queryData.getKey(), "");
            }
        }
        return evaluatedParams;
    }

    public boolean isRelevant(EvaluationContext evalContext) {
        if (relevantExpr == null) {
            return true;
        } else {
            EvaluationContext localEvalContext = evalContext.spawnWithCleanLifecycle();
            Multimap<String, String> evaluatedParams = getEvaluatedParams(localEvalContext, true);
            evaluatedParams.keySet().forEach(key ->
                    localEvalContext.setVariable(key, String.join(" ", evaluatedParams.get(key)))
            );
            String result = RemoteQuerySessionManager.evalXpathExpression(relevantExpr, localEvalContext);
            return "true".equals(result);
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        params = (List<QueryData>)ExtUtil.read(in, new ExtWrapList(new ExtWrapTagged()), pf);
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
