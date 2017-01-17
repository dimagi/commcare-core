package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.utils.GeoPointUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathDistanceFunc extends XPathFuncExpr {
    public static final String NAME = "distance";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathDistanceFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathDistanceFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return distance(evaluatedArgs[0], evaluatedArgs[1]);
    }

    /**
     * Returns the distance between two GeoPointData locations, in meters, given objects to unpack.
     * Ignores altitude and accuracy.
     * Note that the arguments can be strings.
     * Returns -1 if one of the arguments is null or the empty string.
     */
    public static Double distance(Object from, Object to) {
        String unpackedFrom = (String)FunctionUtils.unpack(from);
        String unpackedTo = (String)FunctionUtils.unpack(to);

        if (unpackedFrom == null || "".equals(unpackedFrom) || unpackedTo == null || "".equals(unpackedTo)) {
            return new Double(-1.0);
        }

        // Casting and uncasting seems strange but is consistent with the codebase
        GeoPointData castedFrom = new GeoPointData().cast(new UncastData(unpackedFrom));
        GeoPointData castedTo = new GeoPointData().cast(new UncastData(unpackedTo));

        return new Double(GeoPointUtils.computeDistanceBetween(castedFrom, castedTo));
    }

}
