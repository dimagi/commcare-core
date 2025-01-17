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

public class XPathClosestPolygonPointFunc extends XPathFuncExpr{
    public static final String NAME = "polygon-point";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathClosestPolygonPointFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathClosestPolygonPointFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    /**
     * Returns the point on polygon closest to the geopoint, in "Lat Lng", given objects to unpack.
     * Ignores altitude and accuracy.
     * Note that the arguments can be strings.
     * Returns "" if one of the arguments is null or the empty string.
     */
    @Override
    protected Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return closestPoint(evaluatedArgs[0], evaluatedArgs[1]);
    }

    public static String closestPoint(Object from, Object to) {
        String unpackedFrom = (String)FunctionUtils.unpack(from);
        String unpackedTo = (String)FunctionUtils.unpack(to);
        if (unpackedFrom == null || "".equals(unpackedFrom) || unpackedTo == null || "".equals(unpackedTo)) {
            return "";
        }
        try {
            String[] coordinates=unpackedFrom.split(" ");
            List<Double> polygonList = new ArrayList<Double>();

            for (String coordinate : coordinates) {
                polygonList.add(Double.parseDouble(coordinate));
            }
            // Casting and uncasting seems strange but is consistent with the codebase
            GeoPointData castedTo = new GeoPointData().cast(new UncastData(unpackedTo));
            String closestPointResult=PolygonUtils.getClosestPoint(polygonList,new double[]{castedTo.getLatitude(), castedTo.getLongitude()});

            return closestPointResult;
        } catch (NumberFormatException e) {
            throw new XPathTypeMismatchException("polygon-point() function requires arguments containing " +
                    "numeric values only, but received arguments: " + unpackedFrom + " and " + unpackedTo);
        }
    }
}
