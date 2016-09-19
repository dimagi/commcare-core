package org.javarosa.core.util;

import java.util.Random;
import java.util.regex.Pattern;

/**
 * Static utility functions for mathematical operations
 *
 * @author ctsims
 */
public class MathUtils {
    private static Random r;

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

    // double parsing from http://stackoverflow.com/questions/8564896/fastest-way-to-check-if-a-string-can-be-parsed-to-double-in-java
    final static String Digits     = "(\\p{Digit}+)";
    final static String HexDigits  = "(\\p{XDigit}+)";

    // an exponent is 'e' or 'E' followed by an optionally
    // signed decimal integer.
    final static String Exp        = "[eE][+-]?"+Digits;
    final static String fpRegex    =
            ("[\\x00-\\x20]*"+  // Optional leading "whitespace"
                    "[+-]?(" + // Optional sign character
                    "NaN|" +           // "NaN" string
                    "Infinity|" +      // "Infinity" string

                    // A decimal floating-point string representing a finite positive
                    // number without a leading sign has at most five basic pieces:
                    // Digits . Digits ExponentPart FloatTypeSuffix
                    //
                    // Since this method allows integer-only strings as input
                    // in addition to strings of floating-point literals, the
                    // two sub-patterns below are simplifications of the grammar
                    // productions from the Java Language Specification, 2nd
                    // edition, section 3.10.2.

                    // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                    "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

                    // . Digits ExponentPart_opt FloatTypeSuffix_opt
                    "(\\.("+Digits+")("+Exp+")?)|"+

                    // Hexadecimal strings
                    "((" +
                    // 0[xX] HlxDigits ._opt BinaryExponent FloatTypeSuffix_opt
                    "(0[xX]" + HexDigits + "(\\.)?)|" +

                    // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
                    "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

                    ")[pP][+-]?" + Digits + "))" +
                    "[fFdD]?))" +
                    "[\\x00-\\x20]*");// Optional trailing "whitespace"

    // Safe double parsing - see PR https://github.com/dimagi/commcare-core/pull/405
    public static Double parseDoubleSafe(String string) {
        if (Pattern.matches(fpRegex, string))
            return Double.valueOf(string);
        else {
            throw new NumberFormatException();
        }
    }
}
