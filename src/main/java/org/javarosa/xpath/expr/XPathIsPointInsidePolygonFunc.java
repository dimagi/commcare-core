package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.utils.PolygonUtils;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.locationtech.jts.geom.Polygon;

import java.util.Arrays;

public class XPathIsPointInsidePolygonFunc extends XPathFuncExpr {
    public static final String NAME = "inside-polygon";
    private static final int EXPECTED_ARG_COUNT = 2;

    /**
     * Returns true if the geopoint is inside the polygon, in meters, given objects to unpack.
     * Ignores altitude and accuracy.
     * Note that the arguments can be strings.
     * Returns false if one of the arguments is null or the empty string.
     */
    public XPathIsPointInsidePolygonFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathIsPointInsidePolygonFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }


    @Override
    protected Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return isPointWithinBoundary(evaluatedArgs[0], evaluatedArgs[1]);
    }

    public static boolean isPointWithinBoundary(Object from, Object to) {
        String inputPoint = (String)FunctionUtils.unpack(from);
        String inputPolygon = (String)FunctionUtils.unpack(to);
        if (inputPoint == null || "".equals(inputPoint) || inputPolygon == null || "".equals(inputPolygon)) {
            return false;
        }
        try {
            String[] coordinates = inputPolygon.split(" ");
            Polygon polygon = PolygonUtils.createValidatedPolygon(Arrays.asList(coordinates));
            // Casting and uncasting seems strange but is consistent with the codebase
            GeoPointData pointData = new GeoPointData().cast(new UncastData(inputPoint));

            return PolygonUtils.isPointInsideOrOnPolygon(pointData, polygon);
        } catch (NumberFormatException e) {
            throw new XPathTypeMismatchException("point-in-boundary() function requires arguments containing " +
                    "numeric values only, but received arguments: " + inputPoint + " and " + inputPolygon);
        } catch (IllegalArgumentException e) {
            throw new XPathTypeMismatchException(e.getMessage());
        }
    }
}