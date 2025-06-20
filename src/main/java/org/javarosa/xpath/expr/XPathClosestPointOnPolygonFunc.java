package org.javarosa.xpath.expr;

import org.gavaghan.geodesy.GlobalCoordinates;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.utils.PolygonUtils;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Arrays;
import java.util.List;

/**
 * XPath function "closest-point-on-polygon()" computes the closest point on the boundary of a polygon
 * to a given geographic point.
 *
 * <p><strong>Syntax:</strong></p>
 * <pre>
 *     closest-point-on-polygon(point_coord,polygon_cord)
 * </pre>
 *
 * <p><strong>Parameters:</strong></p>
 * <ul>
 *   <li><code>polygon_coords</code>: A space-separated string of lon/lat pairs (e.g. "'78.041309 27.174957 78
 *   .042574 27.174884 78.042661 27.175493 78.041383 27.175569'")</li>
 *   <li><code>point_coord</code>: A single point as "lon lat eg('78.043 27.175)"</li>
 * </ul>
 *
 * <p><strong>Returns:</strong></p>
 * <p>The closest point on the polygon's boundary to the input point, in "lat lon" format. If the input is
 * invalid, IllegalArgumentException.</p>
 *
 * <p><strong>Recommended Use:</strong></p>
 * <pre>
 *     closest-point-on-polygon('78.041 27.176','78.041309 27.174957 78.042574 27.174884 78.042661 27.175493 78.041383 27.175569')
 * </pre>
 * <p>This example finds the closest point on the polygon to (78.041, 27.176)</p>
 */
public class XPathClosestPointOnPolygonFunc extends XPathFuncExpr {
    public static final String NAME = "closest-point-on-polygon";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathClosestPointOnPolygonFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathClosestPointOnPolygonFunc(XPathExpression[] args) throws XPathSyntaxException {
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
        return closestPointToPolygon(evaluatedArgs[0], evaluatedArgs[1]);
    }

    private static String closestPointToPolygon(Object from, Object to) {
        String inputPoint = (String)FunctionUtils.unpack(from);
        String inputPolygon = (String)FunctionUtils.unpack(to);
        if (inputPoint == null || "".equals(inputPoint) || inputPolygon == null || "".equals(inputPolygon)) {
            throw new XPathException(
                    "closest-point-on-polygon() function requires coordinates of point and polygon");
        }
        try {
            String[] coordinates = inputPolygon.split(" ");
            List<GlobalCoordinates> polygon = PolygonUtils.createPolygon(Arrays.asList(coordinates));
            GeoPointData pointData = new GeoPointData().cast(new UncastData(inputPoint));
            PolygonUtils.validateCoordinates(pointData.getLatitude(), pointData.getLongitude());
            GlobalCoordinates pointCoordinates = new GlobalCoordinates(pointData.getLatitude(),
                    pointData.getLongitude());
            return PolygonUtils.findClosestPoint(pointCoordinates, polygon).toString();
        } catch (NumberFormatException e) {
            throw new XPathTypeMismatchException(
                    "closest-point-on-polygon() function requires arguments containing " +
                            "numeric values only, but received arguments: " + inputPoint + " and " + inputPolygon);
        } catch (IllegalArgumentException e) {
            throw new XPathException(e.getMessage());
        }
    }
}