package org.javarosa.xpath.expr;

import org.javarosa.core.util.CacheTable;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.expr.XPathFuncExpr;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */

public class FunctionUtils {

    private static final CacheTable<String, Double> mDoubleParseCache = new CacheTable<>();
    /**
     * Gets a human readable string representing an xpath nodeset.
     *
     * @param nodeset An xpath nodeset to be visualized
     * @return A string representation of the nodeset's references
     */
    public static String getSerializedNodeset(XPathNodeset nodeset) {
        if (nodeset.size() == 1) {
            return XPathFuncExpr.toString(nodeset);
        }

        StringBuffer sb = new StringBuffer();
        sb.append("{nodeset: ");
        for (int i = 0; i < nodeset.size(); ++i) {
            String ref = nodeset.getRefAt(i).toString(true);
            sb.append(ref);
            if (i != nodeset.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Take in a value (only a string for now, TODO: Extend?) that doesn't
     * have any type information and attempt to infer a more specific type
     * that may assist in equality or comparison operations
     *
     * @param attrValue A typeless data object
     * @return The passed in object in as specific of a type as was able to
     * be identified.
     */
    public static Object InferType(String attrValue) {
        //Throwing exceptions from parsing doubles is _very_ slow, which is the purpose
        //of this cache. In high performant situations, this prevents a ton of overhead.
        Double d = mDoubleParseCache.retrieve(attrValue);
        if(d != null) {
            if(d.isNaN()) {
                return attrValue;
            } else {
                return d;
            }
        }

        try {
            // Don't process strings with scientific notation or +/- Infinity as doubles
            if (XPathFuncExpr.checkForInvalidNumericOrDatestringCharacters(attrValue)) {
                mDoubleParseCache.register(attrValue, new Double(Double.NaN));
                return attrValue;
            }
            Double ret = Double.parseDouble(attrValue);
            mDoubleParseCache.register(attrValue, ret);
            return ret;
        } catch (NumberFormatException ife) {
            //Not a double
            mDoubleParseCache.register(attrValue, new Double(Double.NaN));
        }
        //TODO: What about dates? That is a _super_ expensive
        //operation to be testing, though...
        return attrValue;
    }
}
