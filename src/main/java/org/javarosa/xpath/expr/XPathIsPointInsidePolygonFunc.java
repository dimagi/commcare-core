package org.javarosa.xpath.expr;

import org.gavaghan.geodesy.GlobalCoordinates;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.utils.GeoPointUtils;
import org.javarosa.core.model.utils.PolygonUtils;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Arrays;
import java.util.List;

/**
 * XPath function "is-point-inside-polygon()" determines whether a geographic point lies inside
 * or on the boundary of a polygon.
 *
 * <p><strong>Syntax:</strong></p>
 * <pre>
 *     is-point-inside-polygon(point_coord, polygon_coords)
 * </pre>
 *
 * <p><strong>Parameters:</strong></p>
 * <ul>
 *    <li><code>point_coord</code>: A single point as "lat lon"</li>
 *   <li><code>polygon_coords</code>: A space-separated string of lat/lon pairs (e.g. "lat1 lon1 lat2 lon2 ...")
 *   </li>
 * </ul>
 *
 * <p><strong>Returns:</strong></p>
 * <p><code>true</code> if the point is strictly inside or on the edge/vertex of the polygon,
 * <code>false</code> otherwise, or if inputs are invalid.</p>
 *
 * <p><strong>Recommended Use:</strong></p>
 * <pre>
 *     is-point-inside-polygon('27.174957 78.041309 ','27.174957 78.041309 27.174884 78.042574 27.175493 78.042661 27.175569 78.041383')
 * </pre>
 * <p>This example checks whether the point (27.174957 78.041309) lies inside or on the polygon boundary.
 * It will return <code>true</code> because the point is exactly on one of the polygon's vertices.</p>
 */
public class XPathIsPointInsidePolygonFunc extends XPathFuncExpr {
    public static final String NAME = "is-point-inside-polygon";
    private static final int EXPECTED_ARG_COUNT = 2;

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

    private static boolean isPointWithinBoundary(Object from, Object to) {
        String inputPoint = (String)FunctionUtils.unpack(from);
        String inputPolygon = (String)FunctionUtils.unpack(to);
        if (inputPoint == null || "".equals(inputPoint) || inputPolygon == null || "".equals(inputPolygon)) {
            throw new XPathException(
                    "is-point-inside-polygon() function requires coordinates of point and polygon");
        }
        try {
            String[] coordinates = inputPolygon.split(" ");
            List<GlobalCoordinates> polygon = PolygonUtils.createPolygon(Arrays.asList(coordinates));
            GeoPointData pointData = new GeoPointData().cast(new UncastData(inputPoint));
            GeoPointUtils.validateCoordinates(pointData.getLatitude(), pointData.getLongitude());
            GlobalCoordinates pointCoordinates = new GlobalCoordinates(pointData.getLatitude(),
                    pointData.getLongitude());
            return PolygonUtils.isPointInsideOrOnPolygon(pointCoordinates, polygon);
        } catch (NumberFormatException e) {
            throw new XPathTypeMismatchException(
                    "is-point-inside-polygon() function requires arguments containing " +
                            "numeric values only, but received arguments: " + inputPoint + " and " + inputPolygon);
        } catch (IllegalArgumentException e) {
            throw new XPathException(e.getMessage());
        }
    }
}