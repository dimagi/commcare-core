package org.javarosa.core.util;

import java.util.Random;

public class PropertyUtils {

    /**
     * Generate an RFC 1422 Version 4 UUID.
     *
     * @return a uuid
     */
    public static String genUUID() {
        return randHex(8) + "-" + randHex(4) + "-4" + randHex(3) + "-" + Integer.toString(8 + MathUtils.getRand().nextInt(4), 16) + randHex(3) + "-" + randHex(12);
    }

    /**
     * Create a globally unique identifier string in no particular format
     * with len characters of randomness.
     *
     * @param len The length of the string identifier requested.
     * @return A string containing len characters of random data.
     */
    public static String genGUID(int len) {
        String guid = "";
        for (int i = 0; i < len; i++) { // 25 == 128 bits of entropy
            guid += Integer.toString(MathUtils.getRand().nextInt(36), 36);
        }
        return guid.toUpperCase();
    }

    private static String randHex(int len) {
        String ret = "";
        Random r = MathUtils.getRand();
        for (int i = 0; i < len; ++i) {
            ret += Integer.toString(r.nextInt(16), 16);
        }
        return ret;
    }

    public static String trim(String guid, int len) {
        return guid.substring(0, Math.min(len, guid.length()));
    }
}
