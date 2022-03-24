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
 *
 * ```
 * <data key="device_id" ref="instance('session')/session/context/deviceid"/>
 * ```
 */
public class ValueQueryData implements QueryData {
    private String key;
    private XPathExpression ref;

    @SuppressWarnings("unused")
    public ValueQueryData() {}

    public ValueQueryData(String key, XPathExpression ref) {
        this.key = key;
        this.ref = ref;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Iterable<String> getValues(EvaluationContext context) {
        return Collections.singletonList(FunctionUtils.toString(ref.eval(context)));
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        key = ExtUtil.readString(in);
        ref = (XPathExpression) ExtUtil.read(in, new ExtWrapTagged(), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, key);
        ExtUtil.write(out, new ExtWrapTagged(ref));
    }
}
