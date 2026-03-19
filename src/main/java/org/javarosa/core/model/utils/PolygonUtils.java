package org.javarosa.core.model.utils;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticCurve;
import org.gavaghan.geodesy.GlobalCoordinates;

import java.util.List;

/**
 * Utility class for creating, validating, and interacting with geographic polygons
 * using geodesic (ellipsoid-aware) calculations.
 */
public class PolygonUtils {

    /**
     * Creates a polygon from a flat list of lat/lon strings.
     *
     * @param latLongList Flat list of lat/lon values (e.g., [lat1, lon1, lat2, lon2, ...])
     * @return List of GlobalCoordinates representing the polygon (closed)
     * @throws IllegalArgumentException if input is invalid or polygon is malformed
     */
    public static List<GlobalCoordinates> createPolygon(List<String> latLongList) throws IllegalArgumentException {
        List<GlobalCoordinates> polygon = GeoPointUtils.createPointList(latLongList);

        // Close polygon if not already closed
        if (polygon.size() > 2 && !polygon.get(0).equals(polygon.get(polygon.size() - 1))) {
            polygon.add(new GlobalCoordinates(
                    polygon.get(0).getLatitude(),
                    polygon.get(0).getLongitude()));
        }

        if (polygon.size() < 4) {
            throw new IllegalArgumentException("Polygon must have at least three distinct vertices.");
        }

        return polygon;
    }

    /**
     * Computes the closest point on the polygon border to a given test point using geodesic projection.
     *
     * @param point   The input location
     * @param polygon A closed list of polygon vertices
     * @return String representation of the closest lat/lon pair
     */
    public static String findClosestPoint(GlobalCoordinates point, List<GlobalCoordinates> polygon) {
        GeodeticCalculator calc = new GeodeticCalculator();
        Ellipsoid ellipsoid = Ellipsoid.WGS84;

        double minDist = Double.MAX_VALUE;
        GlobalCoordinates closest = null;

        for (int i = 0; i < polygon.size(); i++) {
            GlobalCoordinates A = polygon.get(i);
            GlobalCoordinates B = polygon.get((i + 1) % polygon.size());

            GlobalCoordinates proj = projectOntoSegment(point, A, B, calc, ellipsoid);
            if (proj == null) continue;

            GeodeticCurve curve = calc.calculateGeodeticCurve(ellipsoid, point, proj);
            double dist = curve.getEllipsoidalDistance();

            if (dist < minDist) {
                minDist = dist;
                closest = proj;
            }
        }

        return closest.getLatitude() + " " + closest.getLongitude();
    }

    /**
     * Projects a test point onto a geodesic segment between two polygon points.
     *
     * @param point     Test point
     * @param A         Segment start
     * @param B         Segment end
     * @param calc      Geodetic calculator
     * @param ellipsoid The ellipsoid reference (WGS84)
     * @return Projected closest point on the segment
     */
    private static GlobalCoordinates projectOntoSegment(
            GlobalCoordinates point,
            GlobalCoordinates A,
            GlobalCoordinates B,
            GeodeticCalculator calc,
            Ellipsoid ellipsoid
    ) {
        if (A.getLatitude() == B.getLatitude() && A.getLongitude() == B.getLongitude()) {
            return A;
        }

        GeodeticCurve AB = calc.calculateGeodeticCurve(ellipsoid, A, B);
        double azimuthAB = AB.getAzimuth();
        double totalLength = AB.getEllipsoidalDistance();

        GeodeticCurve AP = calc.calculateGeodeticCurve(ellipsoid, A, point);
        double azimuthAP = AP.getAzimuth();
        double distanceAP = AP.getEllipsoidalDistance();

        double angleDiff = Math.toRadians(azimuthAP - azimuthAB);
        double projection = distanceAP * Math.cos(angleDiff);

        if (projection <= 0) return A;
        if (projection >= totalLength) return B;

        return calc.calculateEndingGlobalCoordinates(ellipsoid, A, azimuthAB, projection);
    }

    /**
     * Determines if a point lies inside or on the border of a polygon using the ray casting algorithm.
     *
     * @param point   The point to test
     * @param polygon The polygon (list of GlobalCoordinates)
     * @return true if inside or on the edge; false otherwise
     */
    public static boolean isPointInsideOrOnPolygon(GlobalCoordinates point, List<GlobalCoordinates> polygon) {
        int intersectCount = 0;
        int n = polygon.size();

        double testLat = point.getLatitude();
        double testLong = point.getLongitude();

        for (int i = 0; i < n; i++) {
            GlobalCoordinates A = polygon.get(i);
            GlobalCoordinates B = polygon.get((i + 1) % n);

            double latA = A.getLatitude();
            double longA = A.getLongitude();
            double latB = B.getLatitude();
            double longB = B.getLongitude();

            // Vertex check
            if ((testLat == latA && testLong == longA) || (testLat == latB && testLong == longB)) {
                return true;
            }

            // Ray casting
            if (((latA > testLat) != (latB > testLat)) &&
                    (testLong < (longB - longA) * (testLat - latA) / (latB - latA + 1e-10) + longA)) {
                intersectCount++;
            }
        }

        return (intersectCount % 2 == 1);
    }
}