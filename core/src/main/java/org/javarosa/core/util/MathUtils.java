/**
 *
 */
package org.javarosa.core.util;

import java.util.Random;
import java.lang.Math;

/**
 * Static utility functions for mathematical operations
 *
 * @author ctsims
 */
public class MathUtils {
    private static Random r;

    // Constants used for asin calculation
    private static final double PI_L = 1.2246467991473532e-16; // Long bits 0x3ca1a62633145c07L.
    private static final double TWO_27 = 0x8000000;
    private static final double
            PS0 = 0.16666666666666666, // Long bits 0x3fc5555555555555L.
            PS1 = -0.3255658186224009, // Long bits 0xbfd4d61203eb6f7dL.
            PS2 = 0.20121253213486293, // Long bits 0x3fc9c1550e884455L.
            PS3 = -0.04005553450067941, // Long bits 0xbfa48228b5688f3bL.
            PS4 = 7.915349942898145e-4, // Long bits 0x3f49efe07501b288L.
            PS5 = 3.479331075960212e-5, // Long bits 0x3f023de10dfdf709L.
            QS1 = -2.403394911734414, // Long bits 0xc0033a271c8a2d4bL.
            QS2 = 2.0209457602335057, // Long bits 0x40002ae59c598ac8L.
            QS3 = -0.6882839716054533, // Long bits 0xbfe6066c1b8d0159L.
            QS4 = 0.07703815055590194; // Long bits 0x3fb3b8c5b12e9282L.

    //a - b * floor(a / b)
    public static long modLongNotSuck(long a, long b) {
        return ((a % b) + b) % b;
    }

    public static long divLongNotSuck(long a, long b) {
        return (a - modLongNotSuck(a, b)) / b;
    }

    public static Random getRand() {
        if (r == null) {
            r = new Random();
        }
        return r;
    }

    /**
     * Built to run on J2ME, whose Math library does not contain asin.
     * This algorithm is based off http://www.netlib.org/fdlibm/e_asin.c, which carries
     * the following copyright:
     * ====================================================
     * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
     *
     * Developed at SunSoft, a Sun Microsystems, Inc. business.
     * Permission to use, copy, modify, and distribute this
     * software is freely granted, provided that this notice
     * is preserved.
     * ====================================================
     */
    public static double asin(double x) {
        boolean negative = x < 0;
        if (negative)
            x = -x;
        if (! (x <= 1))
            return Double.NaN;
        if (x == 1)
            return negative ? -Math.PI / 2 : Math.PI / 2;
        if (x < 0.5)
        {
            if (x < 1 / TWO_27)
                return negative ? -x : x;
            double t = x * x;
            double p = t * (PS0 + t * (PS1 + t * (PS2 + t * (PS3 + t
                    * (PS4 + t * PS5)))));
            double q = 1 + t * (QS1 + t * (QS2 + t * (QS3 + t * QS4)));
            return negative ? -x - x * (p / q) : x + x * (p / q);
        }
        double w = 1 - x; // 1>|x|>=0.5.
        double t = w * 0.5;
        double p = t * (PS0 + t * (PS1 + t * (PS2 + t * (PS3 + t
                * (PS4 + t * PS5)))));
        double q = 1 + t * (QS1 + t * (QS2 + t * (QS3 + t * QS4)));
        double s = Math.sqrt(t);
        if (x >= 0.975)
        {
            w = p / q;
            t = Math.PI / 2 - (2 * (s + s * w) - PI_L / 2);
        }
        else
        {
            w = (float) s;
            double c = (t - w * w) / (s + w);
            p = 2 * s * (p / q) - (PI_L / 2 - 2 * c);
            q = Math.PI / 4 - 2 * w;
            t = Math.PI / 4 - (p - q);
        }
        return negative ? -t : t;
    }
}
