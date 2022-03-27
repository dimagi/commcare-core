package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;

/**
 * Data class for single value query data elements
 * <pre>{@code
 *  <data
 *    key="device_id"
 *    ref="instance('session')/session/context/deviceid"
 *    exclude="true()"
 * />
 * }</pre>
 *
 * The {@code exclude} attribute is optional.
 */
public class ValueQueryData implements QueryData {
    private String key;
    private XPathExpression ref;
    private XPathExpression excludeExpr;

    @SuppressWarnings("unused")
    public ValueQueryData() {}

    public ValueQueryData(String key, XPathExpression ref, XPathExpression excludeExpr) {
        this.key = key;
        this.ref = ref;
        this.excludeExpr = excludeExpr;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Iterable<String> getValues(EvaluationContext context) {
        if (excludeExpr == null || !(boolean) excludeExpr.eval(context)) {
            return Collections.singletonList(FunctionUtils.toString(ref.eval(context)));
        }
        return Collections.emptyList();
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        key = ExtUtil.readString(in);
        ref = (XPathExpression) ExtUtil.read(in, new ExtWrapTagged(), pf);
        boolean excludeIsNull = ExtUtil.readBool(in);
        if (excludeIsNull) {
            excludeExpr = null;
        } else {
            excludeExpr = (XPathExpression) ExtUtil.read(in, new ExtWrapTagged(), pf);
        }

    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, key);
        ExtUtil.write(out, new ExtWrapTagged(ref));
        boolean excludeIsNull = excludeExpr == null;
        ExtUtil.write(out, excludeIsNull);
        ExtUtil.write(out, new ExtWrapTagged(excludeExpr));
    }
}
