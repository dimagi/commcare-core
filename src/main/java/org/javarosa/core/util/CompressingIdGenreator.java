package org.javarosa.core.util;

/**
 * Created by ctsims on 3/24/2017.
 */

public class CompressingIdGenreator {


    /**
     * Compresses the provided input number into a string.
     *
     * Requires three defined symbol spaces to transform the input string, Growth, Lead, and Body.
     *
     * The growth and lead symbol spaces should be mutually exclusive.
     *
     * Multiple strings generated using the same symbol spaces can be concatenated together in
     * such a way that they will always be unique within their inputs
     *
     * The resulting ID will be of the form
     *
     * [G]*LBBBB
     *
     * Where G is a dynamic number of digits from the "Growth" symbol space, L is a single digit
     * from the "Lead" symbol space, and B is a fixed number of digits from the "Body" symbol
     * space.
     *
     * Note that there is *always* exactly one "Lead" digit. It is acceptable for the count of
     * "Body" digits to be 0.
     *
     * @param input a number to be encoded by the scheme provided. Must be a positive integer
     * @param bodyDigitCount the fixed number of "Body" digits that will be used to encode the
     *                       input value
     */
    public static String generateCompressedIdString(long input,
                                                    String growthSymbols,
                                                    String leadSymbols,
                                                    String bodySymbols,
                                                    int bodyDigitCount) {

        if(growthSymbols.length() == 0 || leadSymbols.length() == 0) {
            throw new IllegalArgumentException(String.format(
                    "Invalid Symbol Space for ID Compression, growth and lead set must both" +
                            " contain at least one symbol" +
                            "\nG[%s] | L[%s] | B[%s]",growthSymbols, leadSymbols, bodySymbols));
        }

        for(char c : growthSymbols.toCharArray()) {
            if(leadSymbols.indexOf(c) != -1) {
                throw new IllegalArgumentException(String.format(
                        "Illegal growth/lead symbol space. The character %s was found in both" +
                                " spaces.", c));
            }
        }


        int leadDigitBase = leadSymbols.length();
        int growthDigitBase = growthSymbols.length();
        int bodyDigitBase = bodySymbols.length();

        long maxSizeOfFixedLengthPortion =
                ((long)Math.pow(bodyDigitBase, bodyDigitCount)) * leadDigitBase;

        int growthDigitCount = 0;

        if (input >= maxSizeOfFixedLengthPortion) {
            double remainingToEncode = input / maxSizeOfFixedLengthPortion;

            growthDigitCount += (int)Math.floor(Math.log(remainingToEncode) /
                    Math.log(growthDigitBase)) + 1;
        }

        int[] digitBases = new int[growthDigitCount + 1 + bodyDigitCount];
        int digit = 0;
        for(int i = 0 ; i < growthDigitCount ; ++i) {
            digitBases[i] = growthDigitBase;
            digit++;
        }

        digitBases[digit] = leadDigitBase;
        digit++;
        for(int i = 0 ; i < bodyDigitCount ; ++i) {
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

        char[] outputGenerator = new char[growthDigitCount + 1 + bodyDigitCount];

        digit = 0;
        for(int i = 0 ; i < growthDigitCount ; ++i) {
            outputGenerator[i] = growthSymbols.charAt(count[i]);
            digit++;
        }
        outputGenerator[digit] = leadSymbols.charAt(count[digit]);
        digit++;
        for(int i = 0 ; i < bodyDigitCount ; ++i) {
            outputGenerator[digit + i] = bodySymbols.charAt(count[digit + i]);
        }

        return new String(outputGenerator);
    }
}
