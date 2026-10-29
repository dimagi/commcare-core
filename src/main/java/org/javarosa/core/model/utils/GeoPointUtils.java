package org.javarosa.core.model.utils;

import org.gavaghan.geodesy.GlobalCoordinates;
import org.javarosa.core.model.data.GeoPointData;

import java.util.ArrayList;
import java.util.List;

/**
 * Static utility methods for GeoPointData.
 *
 * Distance calculation based off Android library:
 * https://github.com/googlemaps/android-maps-utils/blob/master/library/src/com/google/maps/android/SphericalUtil.java
 *
 * @author ftong
 */
public class GeoPointUtils {

    public static final double EARTH_RADIUS = 6371009;  // Earth's radius, in meters

    /**
     * Returns the distance between two GeoPointData locations, in meters.
     * Ignores altitude and accuracy.
     */
    public static double computeDistanceBetween(GeoPointData from, GeoPointData to) {
        return EARTH_RADIUS * distanceRadians(
                Math.toRadians(from.getLatitude()),
                Math.toRadians(from.getLongitude()),
                Math.toRadians(to.getLatitude()),
                Math.toRadians(to.getLongitude())
        );
    }


    /**
     * Returns distance on the unit sphere; the arguments are in radians.
     */
    private static double distanceRadians(double lat1, double lng1, double lat2, double lng2) {
        return arcHav(havDistance(lat1, lat2, lng1 - lng2));
    }


    /**
     * Returns haversine(angle-in-radians).
     * hav(x) == (1 - cos(x)) / 2 == sin(x / 2)^2.
     */
    private static double hav(double x) {
        double sinHalf = Math.sin(x * 0.5);
        return sinHalf * sinHalf;
    }


    /**
     * Computes inverse haversine. Has good numerical stability around 0.
     * arcHav(x) == acos(1 - 2 * x) == 2 * asin(sqrt(x)).
     * The argument must be in [0, 1], and the result is positive.
     */
    private static double arcHav(double x) {
        return 2 * Math.asin(Math.sqrt(x));
    }


    /**
     * Returns hav() of distance from (lat1, lng1) to (lat2, lng2) on the unit sphere.
     */
    private static double havDistance(double lat1, double lat2, double dLng) {
        return hav(lat1 - lat2) + hav(dLng) * Math.cos(lat1) * Math.cos(lat2);
    }

    /**
     * Checks if coordinates are within valid bounds for latitude and longitude.
     *
     * @param latitude  Latitude in degrees
     * @param longitude Longitude in degrees
     * @throws IllegalArgumentException if values are outside geographic bounds
     */
    public static void validateCoordinates(double latitude, double longitude) {
        if ((latitude < -90.0 || latitude > 90.0) || (longitude < -180.0 || longitude > 180.0)) {
            throw new IllegalArgumentException("Invalid coordinates");
        }
    }

    /**
     * Creates a point list from a flat list of lat/lon strings.
     *
     * @param latLongList Flat list of lat/lon values (e.g., [lat1, lon1, lat2, lon2, ...])
     * @return List of GlobalCoordinates representing the list of points
     * @throws IllegalArgumentException if input is invalid (odd number of elements)
     */
    public static List<GlobalCoordinates> createPointList(List<String> latLongList) throws IllegalArgumentException {
        if (latLongList == null || latLongList.size() % 2 != 0) {
            throw new IllegalArgumentException(
                    "Input must contain a list of lat/lng pairs, and must be even-sized.");
        }

        int numPoints = latLongList.size() / 2;
        List<GlobalCoordinates> pointList = new ArrayList<>();

        for (int i = 0; i < numPoints; i++) {
            double latitude = Double.parseDouble(latLongList.get(i * 2));
            double longitude = Double.parseDouble(latLongList.get(i * 2 + 1));
            validateCoordinates(latitude, longitude);
            pointList.add(new GlobalCoordinates(latitude, longitude));
        }

        return pointList;
    }
}
