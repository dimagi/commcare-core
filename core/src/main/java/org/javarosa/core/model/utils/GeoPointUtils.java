package org.javarosa.core.model.utils;

import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.xpath.XPathUnsupportedException;

import static java.lang.Math.*;

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
                toRadians(from.getLatitude()),
                toRadians(from.getLongitude()),
                toRadians(to.getLatitude()),
                toRadians(to.getLongitude())
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
        double sinHalf = sin(x * 0.5);
        return sinHalf * sinHalf;
    }


    /**
     * Computes inverse haversine. Has good numerical stability around 0.
     * arcHav(x) == acos(1 - 2 * x) == 2 * asin(sqrt(x)).
     * The argument must be in [0, 1], and the result is positive.
     */
    private static double arcHav(double x) {
        //#if polish.cldc
        //# throw new XPathUnsupportedException("Sorry, arc sines are not supported on your platform");
        //#else
        return 2 * asin(sqrt(x));
        //#endif
    }


    /**
     * Returns hav() of distance from (lat1, lng1) to (lat2, lng2) on the unit sphere.
     */
    private static double havDistance(double lat1, double lat2, double dLng) {
        return hav(lat1 - lat2) + hav(dLng) * cos(lat1) * cos(lat2);
    }
}
