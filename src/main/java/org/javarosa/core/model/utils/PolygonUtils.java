package org.javarosa.core.model.utils;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticCurve;
import org.gavaghan.geodesy.GlobalCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating, validating, and interacting with geographic polygons
 * using geodesic (ellipsoid-aware) calculations.
 */
public class PolygonUtils {

    /**
     * Creates a polygon from a flat list of lat/lon strings.
     *
     * @param latLngList Flat list of lat/lon values (e.g., [lat1, lon1, lat2, lon2, ...])
     * @return List of GlobalCoordinates representing the polygon (closed)
     * @throws IllegalArgumentException if input is invalid or polygon is malformed
     */
    public static List<GlobalCoordinates> createPolygon(List<String> latLngList) throws IllegalArgumentException {
        if (latLngList == null || latLngList.size() < 6 || latLngList.size() % 2 != 0) {
            throw new IllegalArgumentException(
                    "Input must contain at least three lat/lng pairs (six elements total), and must be even-sized.");
        }

        int numPoints = latLngList.size() / 2;
        List<GlobalCoordinates> polygon = new ArrayList<>();

        for (int i = 0; i < numPoints; i++) {
            double lat = Double.parseDouble(latLngList.get(i * 2));
            double lon = Double.parseDouble(latLngList.get(i * 2 + 1));
            isValidCoordinates(lat, lon);
            polygon.add(new GlobalCoordinates(lat, lon));
        }

        // Close polygon if not already closed
        if (!polygon.get(0).equals(polygon.get(polygon.size() - 1))) {
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
     * Checks if coordinates are within valid bounds for latitude and longitude.
     *
     * @param latitude  Latitude in degrees
     * @param longitude Longitude in degrees
     * @throws IllegalArgumentException if values are outside geographic bounds
     */
    public static void isValidCoordinates(double latitude, double longitude) {
        if ((latitude < -90.0 || latitude > 90.0) || (longitude < -180.0 || longitude > 180.0)) {
            throw new IllegalArgumentException("Invalid polygon coordinates");
        }
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
        double testLon = point.getLongitude();

        for (int i = 0; i < n; i++) {
            GlobalCoordinates A = polygon.get(i);
            GlobalCoordinates B = polygon.get((i + 1) % n);

            double lat1 = A.getLatitude();
            double lon1 = A.getLongitude();
            double lat2 = B.getLatitude();
            double lon2 = B.getLongitude();

            // Vertex check
            if ((testLat == lat1 && testLon == lon1) || (testLat == lat2 && testLon == lon2)) {
                return true;
            }

            // Ray casting
            if (((lat1 > testLat) != (lat2 > testLat)) &&
                    (testLon < (lon2 - lon1) * (testLat - lat1) / (lat2 - lat1 + 1e-10) + lon1)) {
                intersectCount++;
            }
        }

        return (intersectCount % 2 == 1);
    }
}
