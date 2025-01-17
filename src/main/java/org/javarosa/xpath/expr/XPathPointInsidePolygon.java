package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.utils.PolygonUtils;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import java.util.ArrayList;
import java.util.List;

public class XPathPointInsidePolygon extends XPathFuncExpr{
    public static final String NAME = "inside-polygon";
    private static final int EXPECTED_ARG_COUNT = 2;
    /**
     * Returns true if the geopoint is inside the polygon, in meters, given objects to unpack.
     * Ignores altitude and accuracy.
     * Note that the arguments can be strings.
     * Returns false if one of the arguments is null or the empty string.
     */
    public XPathPointInsidePolygon() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathPointInsidePolygon(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }


    @Override
    protected Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return boundaryDistance(evaluatedArgs[0], evaluatedArgs[1]);
    }

    public static boolean boundaryDistance(Object from, Object to) {
        String unpackedFrom = (String)FunctionUtils.unpack(from);
        String unpackedTo = (String)FunctionUtils.unpack(to);
        if (unpackedFrom == null || "".equals(unpackedFrom) || unpackedTo == null || "".equals(unpackedTo)) {
            return false;
        }
        try {
            String[] coordinates=unpackedFrom.split(" ");
            List<Double> polygonList = new ArrayList<Double>();

            for (String coordinate : coordinates) {
                polygonList.add(Double.parseDouble(coordinate));
            }
            // Casting and uncasting seems strange but is consistent with the codebase
            GeoPointData castedTo = new GeoPointData().cast(new UncastData(unpackedTo));
            boolean isInside= PolygonUtils.isPointInsidePolygon(polygonList,new double[]{castedTo.getLatitude(), castedTo.getLongitude()});

            return isInside;
        } catch (NumberFormatException e) {
            throw new XPathTypeMismatchException("point-in-boundary() function requires arguments containing " +
                    "numeric values only, but received arguments: " + unpackedFrom + " and " + unpackedTo);
        }
    }
}