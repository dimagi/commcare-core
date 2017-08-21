package org.javarosa.core.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Static utility functions for mathematical operations
 *
 * @author ctsims
 */
public class MathUtils {
    private static SecureRandom r;

    //a - b * floor(a / b)
    public static long modLongNotSuck(long a, long b) {
        return ((a % b) + b) % b;
    }

    public static long divLongNotSuck(long a, long b) {
        return (a - modLongNotSuck(a, b)) / b;
    }

    public static SecureRandom getRand() {
        if (r == null) {
            r = new SecureRandom();
        }
        return r;
    }
}
