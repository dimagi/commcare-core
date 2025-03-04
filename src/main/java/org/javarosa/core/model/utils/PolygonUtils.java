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
     * This code is written with the help of chatgpt
     */
    public static boolean isPointInsidePolygon(List<Double> polygonPoints, double[] testPoint) {
        int intersectCount = 0;
        int vertexCount = polygonPoints.size() / 2;

        double testLat = testPoint[0];
        double testLng = testPoint[1];

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
     * Finds the minimum distance from a point to the polygon and the closest coordinate on the polygon.
     *
     * @param polygonPoints A list of doubles representing the polygon vertices
     *                      (latitude and longitude pairs).
     * @param testPoint A list of doubles representing the latitude and longitude of the test point.
     * @return A result containing the minimum distance and the closest point on the polygon.
     */
    public static String getClosestPoint(List<Double> polygonPoints, double[] testPoint) {
        int numVertices = polygonPoints.size() / 2;
        double[] closestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < numVertices; i++) {
            // Get the start and end points of the current edge
            double startX = polygonPoints.get(2 * i);
            double startY = polygonPoints.get(2 * i + 1);
            double endX = polygonPoints.get(2 * ((i + 1) % numVertices));
            double endY = polygonPoints.get(2 * ((i + 1) % numVertices) + 1);

            // Find the closest point on this edge
            double[] candidatePoint = getClosestPointOnSegment(
                    startX, startY, endX, endY, testPoint[0], testPoint[1]);
            double distance = distanceBetween(candidatePoint, testPoint);

            // Update the closest point if necessary
            if (distance < minDistance) {
                minDistance = distance;
                closestPoint = candidatePoint;
            }
        }

        // Return the closest point as a space-separated string
        return closestPoint[0] + " " + closestPoint[1];
    }

    private static double[] getClosestPointOnSegment(double startX, double startY, double endX, double endY, double px, double py) {
        double dx = endX - startX;
        double dy = endY - startY;

        if (dx == 0 && dy == 0) {
            // The segment is a single point
            return new double[]{startX, startY};
        }

        // Calculate the projection factor t
        double t = ((px - startX) * dx + (py - startY) * dy) / (dx * dx + dy * dy);

        // Clamp t to the range [0, 1] to stay on the segment
        t = Math.max(0, Math.min(1, t));

        // Compute the closest point
        return new double[]{startX + t * dx, startY + t * dy};
    }

    private static double distanceBetween(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]));
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
}
