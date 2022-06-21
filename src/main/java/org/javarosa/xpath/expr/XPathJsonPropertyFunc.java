package org.javarosa.xpath.expr;

import org.json.JSONObject;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.json.JSONException;
import org.javarosa.xpath.parser.XPathSyntaxException;

/** Utility for hidden values as geocoder receivers
 *
 * @author rcostello
 * @return A String value for the property name passed in if that property exists else a blank String
 */

public class XPathJsonPropertyFunc extends XPathFuncExpr {
    public static final String NAME = "getJsonProperty";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathJsonPropertyFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathJsonPropertyFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return getJsonProperty(FunctionUtils.toString(evaluatedArgs[0]), FunctionUtils.toString(evaluatedArgs[1]));
    }

    /**
     * Returns the value of the property name passed in from the stringified json object passed in.
     * Returns a blank string if the property does not exist on the stringified json object.
     */
    public static String getJsonProperty(String stringifiedJsonObject, String propertyName) throws JSONException {
        JSONObject parsedObject = new JSONObject(stringifiedJsonObject);
        String value = "";
        try {
            value = parsedObject.getString(propertyName);
        } catch (JSONException e) {
            return value;
        }

        return value;
    }
}
