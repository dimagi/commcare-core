package org.commcare.suite.model;

import org.commcare.util.DatumUtil;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Data class for list query data elements
 *
 * ```
 * <data key="case_id_list">
 *   <list nodeset="instance('selected-cases')/session-data/value"
 *         exclude="count(instance('casedb')/casedb/case[@case_id = current()/.]) = 1"
 *         ref="."/>
 * </data>
 * ```
 */
public class ListQueryData implements QueryData {
    private String key;
    private TreeReference nodeset;
    private XPathExpression excludeExpr;
    private XPathPathExpr ref;

    @SuppressWarnings("unused")
    public ListQueryData() {}

    public ListQueryData(String key, TreeReference nodeset, XPathExpression excludeExpr, XPathPathExpr ref) {
        this.key = key;
        this.nodeset = nodeset;
        this.excludeExpr = excludeExpr;
        this.ref = ref;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Iterable<String> getValues(EvaluationContext ec) {
        List<String> values = new ArrayList<String>();

        Vector<TreeReference> result = ec.expandReference(nodeset);

        for (TreeReference node : result) {
            EvaluationContext temp = new EvaluationContext(ec, node);
            if (!(boolean) excludeExpr.eval(temp)) {
                values.add(DatumUtil.getReturnValueFromSelection(node, ref, ec));
            }
        }
        return values;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        key = ExtUtil.readString(in);
        nodeset = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
        excludeExpr = (XPathExpression) ExtUtil.read(in, new ExtWrapTagged(), pf);
        ref = (XPathPathExpr) ExtUtil.read(in, new ExtWrapTagged(), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, key);
        ExtUtil.write(out, nodeset);
        ExtUtil.write(out, new ExtWrapTagged(excludeExpr));
        ExtUtil.write(out, new ExtWrapTagged(ref));
    }
}
