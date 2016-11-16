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
    private static final HashMap<Class, String> funcList = new HashMap<>();

    static {
        funcList.put(XPathDateFunc.class, XPathDateFunc.NAME);
        funcList.put(XpathCoalesceFunc.class, XpathCoalesceFunc.NAME);
        funcList.put(XPathTrueFunc.class, XPathTrueFunc.NAME);
        funcList.put(XPathNowFunc.class, XPathNowFunc.NAME);
        funcList.put(XPathNumberFunc.class, XPathNumberFunc.NAME);
        funcList.put(XPathSelectedFunc.class, XPathSelectedFunc.NAME);
        funcList.put(XPathBooleanFunc.class, XPathBooleanFunc.NAME);
        funcList.put(XPathLogTenFunc.class, XPathLogTenFunc.NAME);
        funcList.put(XPathExpFunc.class, XPathExpFunc.NAME);
        funcList.put(XPathChecklistFunc.class, XPathChecklistFunc.NAME);
        funcList.put(XPathAtanTwoFunc.class, XPathAtanTwoFunc.NAME);
        funcList.put(XPathSubstrFunc.class, XPathSubstrFunc.NAME);
        funcList.put(XPathStringFunc.class, XPathStringFunc.NAME);
        funcList.put(XPathEndsWithFunc.class, XPathEndsWithFunc.NAME);
        funcList.put(XPathDependFunc.class, XPathDependFunc.NAME);
        funcList.put(XPathDoubleFunc.class, XPathDoubleFunc.NAME);
        funcList.put(XPathTanFunc.class, XPathTanFunc.NAME);
        funcList.put(XPathReplaceFunc.class, XPathReplaceFunc.NAME);
        funcList.put(XPathJoinFunc.class, XPathJoinFunc.NAME);
        funcList.put(XPathFloorFunc.class, XPathFloorFunc.NAME);
        funcList.put(XPathPiFunc.class, XPathPiFunc.NAME);
        funcList.put(XPathFormatDateFunc.class, XPathFormatDateFunc.NAME);
        funcList.put(XPathFormatDateForCalendarFunc.class, XPathFormatDateForCalendarFunc.NAME);
        funcList.put(XPathMinFunc.class, XPathMinFunc.NAME);
        funcList.put(XPathSinFunc.class, XPathSinFunc.NAME);
        funcList.put(XPathBooleanFromStringFunc.class, XPathBooleanFromStringFunc.NAME);
        funcList.put(XPathCondFunc.class, XPathCondFunc.NAME);
        funcList.put(XPathSubstringBeforeFunc.class, XPathSubstringBeforeFunc.NAME);
        funcList.put(XPathCeilingFunc.class, XPathCeilingFunc.NAME);
        funcList.put(XPathPositionFunc.class, XPathPositionFunc.NAME);
        funcList.put(XPathStringLengthFunc.class, XPathStringLengthFunc.NAME);
        funcList.put(XPathRandomFunc.class, XPathRandomFunc.NAME);
        funcList.put(XPathMaxFunc.class, XPathMaxFunc.NAME);
        funcList.put(XPathAcosFunc.class, XPathAcosFunc.NAME);
        funcList.put(XPathAsinFunc.class, XPathAsinFunc.NAME);
        funcList.put(XPathIfFunc.class, XPathIfFunc.NAME);
        funcList.put(XPathLowerCaseFunc.class, XPathLowerCaseFunc.NAME);
        funcList.put(XPathIntFunc.class, XPathIntFunc.NAME);
        funcList.put(XPathDistanceFunc.class, XPathDistanceFunc.NAME);
        funcList.put(XPathWeightedChecklistFunc.class, XPathWeightedChecklistFunc.NAME);
        funcList.put(XPathUpperCaseFunc.class, XPathUpperCaseFunc.NAME);
        funcList.put(XPathCosFunc.class, XPathCosFunc.NAME);
        funcList.put(XPathFalseFunc.class, XPathFalseFunc.NAME);
        funcList.put(XPathLogFunc.class, XPathLogFunc.NAME);
        funcList.put(XPathRoundFunc.class, XPathRoundFunc.NAME);
        funcList.put(XPathSubstringAfterFunc.class, XPathSubstringAfterFunc.NAME);
        funcList.put(XPathAbsFunc.class, XPathAbsFunc.NAME);
        funcList.put(XPathTranslateFunc.class, XPathTranslateFunc.NAME);
        funcList.put(XPathCountSelectedFunc.class, XPathCountSelectedFunc.NAME);
        funcList.put(XPathSelectedAtFunc.class, XPathSelectedAtFunc.NAME);
        funcList.put(XPathCountFunc.class, XPathCountFunc.NAME);
        funcList.put(XPathPowFunc.class, XPathPowFunc.NAME);
        funcList.put(XPathContainsFunc.class, XPathContainsFunc.NAME);
        funcList.put(XPathNotFunc.class, XPathNotFunc.NAME);
        funcList.put(XPathSumFunc.class, XPathSumFunc.NAME);
        funcList.put(XPathRegexFunc.class, XPathRegexFunc.NAME);
        funcList.put(XPathAtanFunc.class, XPathAtanFunc.NAME);
        funcList.put(XPathStartsWithFunc.class, XPathStartsWithFunc.NAME);
        funcList.put(XPathTodayFunc.class, XPathTodayFunc.NAME);
        funcList.put(XPathConcatFunc.class, XPathConcatFunc.NAME);
        funcList.put(XPathSqrtFunc.class, XPathSqrtFunc.NAME);
        funcList.put(XPathUuidFunc.class, XPathUuidFunc.NAME);
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
        return new ArrayList<>(funcList.values());
    }

    public static HashMap<Class, String> getXPathFuncListMap() {
        return funcList;
    }
}
