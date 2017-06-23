package org.javarosa.xpath.expr;

import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.util.CacheTable;
import org.javarosa.core.util.MathUtils;
import org.javarosa.xpath.IExprDataType;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class FunctionUtils {
    private static final HashMap<String, Class> funcList = new HashMap<>();

    static {
        funcList.put(XPathDateFunc.NAME, XPathDateFunc.class);
        funcList.put(XpathCoalesceFunc.NAME, XpathCoalesceFunc.class);
        funcList.put(XPathTrueFunc.NAME, XPathTrueFunc.class);
        funcList.put(XPathNowFunc.NAME, XPathNowFunc.class);
        funcList.put(XPathNumberFunc.NAME, XPathNumberFunc.class);
        funcList.put(XPathSelectedFunc.NAME, XPathSelectedFunc.class);
        funcList.put(XPathBooleanFunc.NAME, XPathBooleanFunc.class);
        funcList.put(XPathLogTenFunc.NAME, XPathLogTenFunc.class);
        funcList.put(XPathExpFunc.NAME, XPathExpFunc.class);
        funcList.put(XPathChecklistFunc.NAME, XPathChecklistFunc.class);
        funcList.put(XPathAtanTwoFunc.NAME, XPathAtanTwoFunc.class);
        funcList.put(XPathSubstrFunc.NAME, XPathSubstrFunc.class);
        funcList.put(XPathStringFunc.NAME, XPathStringFunc.class);
        funcList.put(XPathEndsWithFunc.NAME, XPathEndsWithFunc.class);
        funcList.put(XPathDependFunc.NAME, XPathDependFunc.class);
        funcList.put(XPathDoubleFunc.NAME, XPathDoubleFunc.class);
        funcList.put(XPathTanFunc.NAME, XPathTanFunc.class);
        funcList.put(XPathReplaceFunc.NAME, XPathReplaceFunc.class);
        funcList.put(XPathJoinFunc.NAME, XPathJoinFunc.class);
        funcList.put(XPathFloorFunc.NAME, XPathFloorFunc.class);
        funcList.put(XPathPiFunc.NAME, XPathPiFunc.class);
        funcList.put(XPathFormatDateFunc.NAME, XPathFormatDateFunc.class);
        funcList.put(XPathFormatDateForCalendarFunc.NAME, XPathFormatDateForCalendarFunc.class);
        funcList.put(XPathMinFunc.NAME, XPathMinFunc.class);
        funcList.put(XPathSinFunc.NAME, XPathSinFunc.class);
        funcList.put(XPathBooleanFromStringFunc.NAME, XPathBooleanFromStringFunc.class);
        funcList.put(XPathCondFunc.NAME, XPathCondFunc.class);
        funcList.put(XPathSubstringBeforeFunc.NAME, XPathSubstringBeforeFunc.class);
        funcList.put(XPathCeilingFunc.NAME, XPathCeilingFunc.class);
        funcList.put(XPathPositionFunc.NAME, XPathPositionFunc.class);
        funcList.put(XPathStringLengthFunc.NAME, XPathStringLengthFunc.class);
        funcList.put(XPathRandomFunc.NAME, XPathRandomFunc.class);
        funcList.put(XPathMaxFunc.NAME, XPathMaxFunc.class);
        funcList.put(XPathAcosFunc.NAME, XPathAcosFunc.class);
        funcList.put(XPathAsinFunc.NAME, XPathAsinFunc.class);
        funcList.put(XPathIfFunc.NAME, XPathIfFunc.class);
        funcList.put(XPathLowerCaseFunc.NAME, XPathLowerCaseFunc.class);
        funcList.put(XPathIntFunc.NAME, XPathIntFunc.class);
        funcList.put(XPathDistanceFunc.NAME, XPathDistanceFunc.class);
        funcList.put(XPathWeightedChecklistFunc.NAME, XPathWeightedChecklistFunc.class);
        funcList.put(XPathUpperCaseFunc.NAME, XPathUpperCaseFunc.class);
        funcList.put(XPathCosFunc.NAME, XPathCosFunc.class);
        funcList.put(XPathFalseFunc.NAME, XPathFalseFunc.class);
        funcList.put(XPathLogFunc.NAME, XPathLogFunc.class);
        funcList.put(XPathRoundFunc.NAME, XPathRoundFunc.class);
        funcList.put(XPathSubstringAfterFunc.NAME, XPathSubstringAfterFunc.class);
        funcList.put(XPathAbsFunc.NAME, XPathAbsFunc.class);
        funcList.put(XPathTranslateFunc.NAME, XPathTranslateFunc.class);
        funcList.put(XPathCountSelectedFunc.NAME, XPathCountSelectedFunc.class);
        funcList.put(XPathSelectedAtFunc.NAME, XPathSelectedAtFunc.class);
        funcList.put(XPathCountFunc.NAME, XPathCountFunc.class);
        funcList.put(XPathPowFunc.NAME, XPathPowFunc.class);
        funcList.put(XPathContainsFunc.NAME, XPathContainsFunc.class);
        funcList.put(XPathNotFunc.NAME, XPathNotFunc.class);
        funcList.put(XPathSumFunc.NAME, XPathSumFunc.class);
        funcList.put(XPathRegexFunc.NAME, XPathRegexFunc.class);
        funcList.put(XPathAtanFunc.NAME, XPathAtanFunc.class);
        funcList.put(XPathStartsWithFunc.NAME, XPathStartsWithFunc.class);
        funcList.put(XPathTodayFunc.NAME, XPathTodayFunc.class);
        funcList.put(XPathConcatFunc.NAME, XPathConcatFunc.class);
        funcList.put(XPathSqrtFunc.NAME, XPathSqrtFunc.class);
        funcList.put(XPathUuidFunc.NAME, XPathUuidFunc.class);
        funcList.put(XPathIdCompressFunc.NAME, XPathIdCompressFunc.class);
        funcList.put(XPathJoinChunkFunc.NAME, XPathJoinChunkFunc.class);
        funcList.put(XPathChecksumFunc.NAME, XPathChecksumFunc.class);
    }

    private static final CacheTable<String, Double> mDoubleParseCache = new CacheTable<>();
    /**
     * Gets a human readable string representing an xpath nodeset.
     *
     * @param nodeset An xpath nodeset to be visualized
     * @return A string representation of the nodeset's references
     */
    public static String getSerializedNodeset(XPathNodeset nodeset) {
        if (nodeset.size() == 1) {
            return toString(nodeset);
        }

        StringBuffer sb = new StringBuffer();
        sb.append("{nodeset: ");
        for (int i = 0; i < nodeset.size(); ++i) {
            String ref = nodeset.getRefAt(i).toString(true);
            sb.append(ref);
            if (i != nodeset.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Take in a value (only a string for now, TODO: Extend?) that doesn't
     * have any type information and attempt to infer a more specific type
     * that may assist in equality or comparison operations
     *
     * @param attrValue A typeless data object
     * @return The passed in object in as specific of a type as was able to
     * be identified.
     */
    public static Object InferType(String attrValue) {
        //Throwing exceptions from parsing doubles is _very_ slow, which is the purpose
        //of this cache. In high performant situations, this prevents a ton of overhead.
        Double d = mDoubleParseCache.retrieve(attrValue);
        if(d != null) {
            if(d.isNaN()) {
                return attrValue;
            } else {
                return d;
            }
        }

        try {
            // Don't process strings with scientific notation or +/- Infinity as doubles
            if (checkForInvalidNumericOrDatestringCharacters(attrValue)) {
                mDoubleParseCache.register(attrValue, new Double(Double.NaN));
                return attrValue;
            }
            Double ret = Double.parseDouble(attrValue);
            mDoubleParseCache.register(attrValue, ret);
            return ret;
        } catch (NumberFormatException ife) {
            //Not a double
            mDoubleParseCache.register(attrValue, new Double(Double.NaN));
        }
        //TODO: What about dates? That is a _super_ expensive
        //operation to be testing, though...
        return attrValue;
    }

    /**
     * ***** HANDLERS FOR BUILT-IN FUNCTIONS ********
     *
     * the functions below are the handlers for the built-in xpath function suite
     *
     * if you add a function to the suite, it should adhere to the following pattern:
     *
     * * the function takes in its arguments as objects (DO NOT cast the arguments when calling
     * the handler up in eval() (i.e., return stringLength((String)argVals[0])  <--- NO!)
     *
     * * the function converts the generic argument(s) to the desired type using the built-in
     * xpath type conversion functions (toBoolean(), toNumeric(), toString(), toDate())
     *
     * * the function MUST return an object of type Boolean, Double, String, or Date; it may
     * never return null (instead return the empty string or NaN)
     *
     * * the function may throw exceptions, but should try as hard as possible not to, and if
     * it must, strive to make it an XPathException
     */

    /**
     * convert a value to a boolean using xpath's type conversion rules
     */
    public static Boolean toBoolean(Object o) {
        Boolean val = null;

        o = unpack(o);

        if (o instanceof Boolean) {
            val = (Boolean)o;
        } else if (o instanceof Double) {
            double d = (Double)o;
            val = Math.abs(d) > 1.0e-12 && !Double.isNaN(d);
        } else if (o instanceof String) {
            String s = (String)o;
            val = s.length() > 0;
        } else if (o instanceof Date) {
            val = Boolean.TRUE;
        } else if (o instanceof IExprDataType) {
            val = ((IExprDataType)o).toBoolean();
        }

        if (val != null) {
            return val;
        } else {
            throw new XPathTypeMismatchException("converting to boolean");
        }
    }

    public static Double toDouble(Object o) {
        if (o instanceof Date) {
            return DateUtils.fractionalDaysSinceEpoch((Date)o);
        } else {
            return toNumeric(o);
        }
    }

    /**
     * Convert a value to a number using xpath's type conversion rules (note that xpath itself makes
     * no distinction between integer and floating point numbers)
     */
    public static Double toNumeric(Object o) {
        Double val = null;

        o = unpack(o);

        if (o instanceof Boolean) {
            val = new Double(((Boolean)o).booleanValue() ? 1 : 0);
        } else if (o instanceof Double) {
            val = (Double)o;
        } else if (o instanceof String) {
            String s = ((String)o).trim();
            if (checkForInvalidNumericOrDatestringCharacters(s)) {
                return new Double(Double.NaN);
            }
            try {
                val = new Double(Double.parseDouble(s));
            } catch (NumberFormatException nfe) {
                try {
                    val = attemptDateConversion(s);
                } catch (XPathTypeMismatchException e) {
                    val = new Double(Double.NaN);
                }
            }
        } else if (o instanceof Date) {
            val = new Double(DateUtils.daysSinceEpoch((Date)o));
        } else if (o instanceof IExprDataType) {
            val = ((IExprDataType)o).toNumeric();
        }

        if (val != null) {
            return val;
        } else {
            throw new XPathTypeMismatchException("converting '" + (o == null ? "null" : o.toString()) + "' to numeric");
        }
    }

    /**
     * The xpath spec doesn't recognize scientific notation, or +/-Infinity when converting a
     * string to a number
     */
    protected static boolean checkForInvalidNumericOrDatestringCharacters(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '-' && c != '.' && (c < '0' || c > '9')) {
                return true;
            }
        }
        return false;
    }

    private static Double attemptDateConversion(String s) {
        Object o = toDate(s);
        if (o instanceof Date) {
            return toNumeric(o);
        } else {
            throw new XPathTypeMismatchException();
        }
    }

    /**
     * convert a number to an integer by truncating the fractional part. if non-numeric, coerce the
     * value to a number first. note that the resulting return value is still a Double, as required
     * by the xpath engine
     */
    public static Double toInt(Object o) {
        Double val = toNumeric(o);

        if (val.isInfinite() || val.isNaN()) {
            return val;
        } else if (val >= Long.MAX_VALUE || val <= Long.MIN_VALUE) {
            return val;
        } else {
            long l = val.longValue();
            Double dbl = new Double(l);
            if (l == 0 && (val < 0. || val.equals(new Double(-0.)))) {
                dbl = new Double(-0.);
            }
            return dbl;
        }
    }

    /**
     * convert a value to a string using xpath's type conversion rules
     */
    public static String toString(Object o) {
        String val = null;

        o = unpack(o);

        if (o instanceof Boolean) {
            val = ((Boolean)o ? "true" : "false");
        } else if (o instanceof Double) {
            double d = (Double)o;
            if (Double.isNaN(d)) {
                val = "NaN";
            } else if (Math.abs(d) < 1.0e-12) {
                val = "0";
            } else if (Double.isInfinite(d)) {
                val = (d < 0 ? "-" : "") + "Infinity";
            } else if (Math.abs(d - (int)d) < 1.0e-12) {
                val = String.valueOf((int)d);
            } else {
                val = String.valueOf(d);
            }
        } else if (o instanceof String) {
            val = (String)o;
        } else if (o instanceof Date) {
            val = DateUtils.formatDate((Date)o, DateUtils.FORMAT_ISO8601);
        } else if (o instanceof IExprDataType) {
            val = o.toString();
        }

        if (val != null) {
            return val;
        } else {
            if (o == null) {
                throw new XPathTypeMismatchException("attempt to cast null value to string");
            } else {
                throw new XPathTypeMismatchException("converting object of type " + o.getClass().toString() + " to string");
            }
        }
    }

    /**
     * convert a value to a date. note that xpath has no intrinsic representation of dates, so this
     * is off-spec. dates convert to strings as 'yyyy-mm-dd', convert to numbers as # of days since
     * the unix epoch, and convert to booleans always as 'true'
     *
     * string and int conversions are reversable, however:
     * * cannot convert bool to date
     * * empty string and NaN (xpath's 'null values') go unchanged, instead of being converted
     * into a date (which would cause an error, since Date has no null value (other than java
     * null, which the xpath engine can't handle))
     * * note, however, than non-empty strings that aren't valid dates _will_ cause an error
     * during conversion
     */
    public static Object toDate(Object o) {
        o = unpack(o);

        if (o instanceof Double) {
            Double n = toInt(o);

            if (n.isNaN()) {
                return n;
            }

            if (n.isInfinite() || n > Integer.MAX_VALUE || n < Integer.MIN_VALUE) {
                throw new XPathTypeMismatchException("converting out-of-range value to date");
            }

            return DateUtils.dateAdd(DateUtils.getDate(1970, 1, 1), n.intValue());
        } else if (o instanceof String) {
            String s = (String)o;

            if (s.length() == 0) {
                return s;
            }

            Date d = DateUtils.parseDateTime(s);
            if (d == null) {
                throw new XPathTypeMismatchException("converting string " + s + " to date");
            } else {
                return d;
            }
        } else if (o instanceof Date) {
            return DateUtils.roundDate((Date)o);
        } else {
            String type = o == null ? "null" : o.getClass().getName();
            throw new XPathTypeMismatchException("converting unexpected type " + type + " to date");
        }
    }

    protected static Date expandDateSafe(Object dateObject) {
        if (!(dateObject instanceof Date)) {
            // try to expand this out of a nodeset
            dateObject = toDate(dateObject);
        }
        if (dateObject instanceof Date) {
            return (Date)dateObject;
        } else {
            return null;
        }
    }

    protected static Object[] subsetArgList(Object[] args, int start) {
        return subsetArgList(args, start, 1);
    }

    /**
     * return a subset of an argument list as a new arguments list
     *
     * @param start index to start at
     * @param skip  sub-list will contain every nth argument, where n == skip (default: 1)
     */
    protected static Object[] subsetArgList(Object[] args, int start, int skip) {
        if (start > args.length || skip < 1) {
            throw new RuntimeException("error in subsetting arglist");
        }

        Object[] subargs = new Object[(int)MathUtils.divLongNotSuck(args.length - start - 1, skip) + 1];
        for (int i = start, j = 0; i < args.length; i += skip, j++) {
            subargs[j] = args[i];
        }

        return subargs;
    }

    public static Object unpack(Object o) {
        if (o instanceof XPathNodeset) {
            return ((XPathNodeset)o).unpack();
        } else {
            return o;
        }
    }

    /**
     * Perform toUpperCase or toLowerCase on given object.
     */
    protected static String normalizeCase(Object o, boolean toUpper) {
        String s = toString(o);
        if (toUpper) {
            return s.toUpperCase();
        }
        return s.toLowerCase();
    }

    /**
     * Get list of base xpath functions
     *
     * (Used in formplayer for function auto-completion)
     */
    @SuppressWarnings("unused")
    public static List<String> xPathFuncList() {
        return new ArrayList<>(funcList.keySet());
    }

    public static HashMap<String, Class> getXPathFuncListMap() {
        return funcList;
    }
}
