package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.utils.GeoPointUtils;
import org.javarosa.core.model.utils.PolygonUtils;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.List;

public class XPathBoundaryDistanceFunc extends XPathFuncExpr{
    public static final String NAME = "boundaryDistance";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathBoundaryDistanceFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathBoundaryDistanceFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }


    @Override
    protected Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return boundaryDistance(evaluatedArgs[0], evaluatedArgs[1]);
    }

    public static Double boundaryDistance(Object from, Object to) {
        String unpackedFrom = (String)FunctionUtils.unpack(from);
        String unpackedTo = (String)FunctionUtils.unpack(to);
        if (unpackedFrom == null || "".equals(unpackedFrom) || unpackedTo == null || "".equals(unpackedTo)) {
            return Double.valueOf(-1.0);
        }
        try {
            String[] coordinates=unpackedFrom.split(" ");
            List<Double> polygonList = new ArrayList<Double>();

            for (String coordinate : coordinates) {
                polygonList.add(Double.parseDouble(coordinate));
            }
            // Casting and uncasting seems strange but is consistent with the codebase
            GeoPointData castedTo = new GeoPointData().cast(new UncastData(unpackedTo));
            double distance=PolygonUtils.distanceToClosestBoundary(polygonList,new double[]{castedTo.getLatitude(), castedTo.getLongitude()});

            return distance;
        } catch (NumberFormatException e) {
            throw new XPathTypeMismatchException("distance() function requires arguments containing " +
                    "numeric values only, but received arguments: " + unpackedFrom + " and " + unpackedTo);
        }
    }
}
