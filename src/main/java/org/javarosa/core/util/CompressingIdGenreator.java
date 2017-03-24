package org.javarosa.core.util;

/**
 * Created by ctsims on 3/24/2017.
 */

public class CompressingIdGenreator {


    /**
     * Compresses the provided input number into a string which is unique and can be
     * concatenated with another string created according to the same scheme in a
     * way such that there is no overlap between any strings created in such a manner.
     */
    public static String generateCompressedIdString(long input,
                                                    String growthDigits,
                                                    String leadDigits,
                                                    String bodyDigits,
                                                    int fixedBodyLength) {

        int leadDigitBase = leadDigits.length();
        int growthDigitBase = growthDigits.length();
        int bodyDigitBase = bodyDigits.length();

        long maxFixedLengthString = ((long)Math.pow(bodyDigitBase, fixedBodyLength)) * leadDigitBase;

        int growthDigitCount = 0;

        if (input >= maxFixedLengthString) {
            double remainingToEncode = input / maxFixedLengthString;

            growthDigitCount += (int)Math.floor(Math.log(remainingToEncode) /
                    Math.log(growthDigitBase)) + 1;
        }

        int[] digitBases = new int[growthDigitCount + 1 + fixedBodyLength];
        int digit = 0;
        for(int i = 0 ; i < growthDigitCount ; ++i) {
            digitBases[i] = growthDigitBase;
            digit++;
        }

        digitBases[digit] = leadDigitBase;
        digit++;
        for(int i = 0 ; i < fixedBodyLength ; ++i) {
            digitBases[digit + i] = bodyDigitBase;
        }

        long[] divisors = new long[digitBases.length];
        divisors[divisors.length -1] = 1;
        for(int i = divisors.length -2; i >= 0; i--) {
            divisors[i] = divisors[i + 1] * digitBases[i + 1];
        }

        long remainder = input;
        int[] count = new int[digitBases.length];
        for(int i = 0 ; i < digitBases.length; i++) {
            count[i] = (int)Math.floor(remainder / divisors[i]);
            remainder = remainder % divisors[i];
        }
        if(remainder != 0) {
            throw new RuntimeException("Invalid ID Generation! Number was not fully encoded");
        }

        char[] outputGenerator = new char[growthDigitCount + 1 + fixedBodyLength];
        digit = 0;
        for(int i = 0 ; i < growthDigitCount ; ++i) {
            outputGenerator[i] = growthDigits.charAt(count[i]);
            digit++;
        }
        outputGenerator[digit] = leadDigits.charAt(count[digit]);
        digit++;
        for(int i = 0 ; i < fixedBodyLength ; ++i) {
            outputGenerator[digit + i] = bodyDigits.charAt(count[digit + i]);
        }
        return new String(outputGenerator);
    }
}
