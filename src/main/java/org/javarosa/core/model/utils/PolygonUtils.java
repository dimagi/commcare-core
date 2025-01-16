package org.javarosa.core.model.utils;
import java.util.List;

public class PolygonUtils {

    /**
     * Determines if a point is inside a polygon.
     *
     * @param polygonPoints A list of doubles representing the polygon vertices
     *                      (latitude and longitude pairs).
     * @param testPoint A list of doubles representing the latitude and longitude of the test point.
     * @return true if the point is inside the polygon, false otherwise.
     */
    public static boolean isPointInsidePolygon(List<Double> polygonPoints, List<Double> testPoint) {
        int intersectCount = 0;
        int vertexCount = polygonPoints.size() / 2;

        double testLat = testPoint.get(0);
        double testLng = testPoint.get(1);

        for (int i = 0; i < vertexCount; i++) {
            double lat1 = polygonPoints.get(2 * i);
            double lng1 = polygonPoints.get(2 * i + 1);
            double lat2 = polygonPoints.get((2 * ((i + 1) % vertexCount)));
            double lng2 = polygonPoints.get((2 * ((i + 1) % vertexCount)) + 1);

            if (rayIntersectsEdge(testLat, testLng, lat1, lng1, lat2, lng2)) {
                intersectCount++;
            }
        }

        return (intersectCount % 2 == 1);
    }

    /**
     * Checks if a ray starting from the test point intersects the edge defined by two vertices.
     */
    private static boolean rayIntersectsEdge(double testLat, double testLng, double lat1, double lng1, double lat2, double lng2) {
        if (lat1 > lat2) {
            double tempLat = lat1, tempLng = lng1;
            lat1 = lat2;
            lng1 = lng2;
            lat2 = tempLat;
            lng2 = tempLng;
        }

        if (testLat < lat1 || testLat > lat2) {
            return false;
        }

        if (testLng > Math.max(lng1, lng2)) {
            return false;
        }

        if (testLng < Math.min(lng1, lng2)) {
            return true;
        }

        double slope = (lng2 - lng1) / (lat2 - lat1);
        double intersectLng = lng1 + (testLat - lat1) * slope;

        return testLng < intersectLng;
    }

    /**
     * Calculates the distance from a point to the closest boundary of the polygon.
     *
     * @param polygonPoints A list of doubles representing the polygon vertices
     *                      (latitude and longitude pairs).
     * @param testPoint A list of doubles representing the latitude and longitude of the test point.
     * @return The distance from the test point to the closest edge of the polygon.
     */
    public static double distanceToClosestBoundary(List<Double> polygonPoints, double[] testPoint) {
        double minDistance = Double.MAX_VALUE;

        int vertexCount = polygonPoints.size() / 2;
        double testLat = testPoint[0];
        double testLng = testPoint[1];

        for (int i = 0; i < vertexCount; i++) {
            double lat1 = polygonPoints.get(2 * i);
            double lng1 = polygonPoints.get(2 * i + 1);
            double lat2 = polygonPoints.get((2 * ((i + 1) % vertexCount)));
            double lng2 = polygonPoints.get((2 * ((i + 1) % vertexCount)) + 1);

            double distance = pointToSegmentDistance(testLat, testLng, lat1, lng1, lat2, lng2);
            minDistance = Math.min(minDistance, distance);
        }

        return minDistance;
    }

    /**
     * Calculates the shortest distance from a point to a line segment.
     */
    private static double pointToSegmentDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;

        if (dx == 0 && dy == 0) {
            // The segment is a point
            return Math.sqrt(Math.pow(px - x1, 2) + Math.pow(py - y1, 2));
        }

        // Calculate the projection of the point onto the line
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);

        if (t < 0) {
            // Closest to the first endpoint
            return Math.sqrt(Math.pow(px - x1, 2) + Math.pow(py - y1, 2));
        } else if (t > 1) {
            // Closest to the second endpoint
            return Math.sqrt(Math.pow(px - x2, 2) + Math.pow(py - y2, 2));
        } else {
            // Closest to a point on the segment
            double projX = x1 + t * dx;
            double projY = y1 + t * dy;
            return Math.sqrt(Math.pow(px - projX, 2) + Math.pow(py - projY, 2));
        }
    }
}
