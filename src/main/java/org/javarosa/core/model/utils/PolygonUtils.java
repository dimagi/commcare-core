package org.javarosa.core.model.utils;

import org.javarosa.core.model.data.GeoPointData;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.operation.valid.IsValidOp;

import java.util.List;

/**
 * Utility class for creating, validating, and interacting with geographic polygons.
 */
public class PolygonUtils {

    /**
     * Creates a valid polygon from a list of latitude and longitude strings.
     *
     * @param latLngList List of alternating latitude and longitude strings (e.g., [lat1, lng1, lat2, lng2, ...])
     * @return a valid {@link Polygon} object
     * @throws IllegalArgumentException if the input is malformed or the polygon is invalid
     */
    public static Polygon createValidatedPolygon(List<String> latLngList) throws IllegalArgumentException {
        if (latLngList == null || latLngList.size() < 6 || latLngList.size() % 2 != 0) {
            throw new IllegalArgumentException(
                    "Input must contain at least three lat/lng pairs (six elements total), and must be "
                            + "even-sized.");
        }

        GeometryFactory geometryFactory = new GeometryFactory();
        int numPoints = latLngList.size() / 2;
        Coordinate[] coordinates = new Coordinate[numPoints + 1];

        for (int i = 0; i < numPoints; i++) {
            double latitude = Double.parseDouble(latLngList.get(i * 2));
            double longitude = Double.parseDouble(latLngList.get(i * 2 + 1));
            coordinates[i] = new Coordinate(longitude, latitude); // JTS uses x=longitude, y=latitude
        }

        // Close the polygon
        coordinates[numPoints] = coordinates[0];

        LinearRing shell = geometryFactory.createLinearRing(coordinates);
        Polygon polygon = geometryFactory.createPolygon(shell);

        IsValidOp validator = new IsValidOp(polygon);
        if (!validator.isValid()) {
            throw new IllegalArgumentException("Invalid polygon: " + validator.getValidationError().getMessage());
        }

        return polygon;
    }

    /**
     * Determines if a given point lies inside or on the boundary of the provided polygon.
     *
     * @param polygon   The {@link Polygon} to test against.
     * @param pointData A {@link GeoPointData} representing the test point.
     * @return true if the point is inside or on the polygon, false otherwise.
     */
    public static boolean isPointInsideOrOnPolygon(Polygon polygon, GeoPointData pointData) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Point point = geometryFactory.createPoint(
                new Coordinate(pointData.getLongitude(), pointData.getLatitude()));
        return polygon.covers(point);
    }

    /**
     * Finds the closest point on the polygon from a given external point.
     *
     * @param polygon   The {@link Polygon} to check against.
     * @param pointData A {@link GeoPointData} representing the external point.
     * @return A string in the format "lat lng" representing the closest point on the polygon.
     */
    public static String getClosestPointOnPolygon(Polygon polygon, GeoPointData pointData) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Point externalPoint = geometryFactory.createPoint(
                new Coordinate(pointData.getLongitude(), pointData.getLatitude()));
        Coordinate[] nearestPoints = DistanceOp.nearestPoints(polygon, externalPoint);
        Coordinate closest = nearestPoints[0];
        return closest.y + " " + closest.x; // Return in "lat lng" format
    }
}