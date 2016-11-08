package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.condition.pivot.UnpivotableExpressionException;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.util.MathUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.IExprDataType;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

/**
 * Representation of an xpath function expression.
 *
 * All of the built-in xpath functions are included here, as well as the xpath type conversion logic
 *
 * Evaluation of functions can delegate out to custom function handlers that must be registered at
 * runtime.
 */
public abstract class XPathFuncExpr extends XPathExpression {
    protected String id;
    public XPathExpression[] args;
    protected Object[] evaluatedArgs;
    protected int expectedArgCount;
    private boolean evaluateArgsFirst;

    @SuppressWarnings("unused")
    public XPathFuncExpr() {
        // for deserialization
    }

    public XPathFuncExpr(String id, XPathExpression[] args,
                         int expectedArgCount, boolean evaluateArgsFirst)
            throws XPathSyntaxException {
        this.id = id;
        this.args = args;
        this.expectedArgCount = expectedArgCount;
        this.evaluateArgsFirst = evaluateArgsFirst;

        validateArgCount();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("{func-expr:");
        sb.append(id);
        sb.append(",{");
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i].toString());
            if (i < args.length - 1)
                sb.append(",");
        }
        sb.append("}}");

        return sb.toString();
    }

    @Override
    public String toPrettyString() {
        StringBuffer sb = new StringBuffer();
        sb.append(id).append("(");
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i].toPrettyString());
            if (i < args.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XPathFuncExpr) {
            XPathFuncExpr x = (XPathFuncExpr)o;

            //Shortcuts for very easily comprable values
            //We also only return "True" for methods we expect to return the same thing. This is not good
            //practice in Java, since o.equals(o) will return false. We should evaluate that differently.
            //Dec 8, 2011 - Added "uuid", since we should never assume one uuid equals another
            //May 6, 2013 - Added "random", since two calls asking for a random
            if (!id.equals(x.id) || args.length != x.args.length || id.equals("uuid") || id.equals("random")) {
                return false;
            }

            return ExtUtil.arrayEquals(args, x.args, false);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int argsHash = 0;
        for (XPathExpression arg : args) {
            argsHash ^= arg.hashCode();
        }
        return id.hashCode() ^ argsHash;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        expectedArgCount = ExtUtil.readInt(in);
        evaluateArgsFirst = ExtUtil.readBool(in);

        Vector v = (Vector)ExtUtil.read(in, new ExtWrapListPoly(), pf);
        args = new XPathExpression[v.size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = (XPathExpression)v.elementAt(i);
        }
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, expectedArgCount);
        ExtUtil.write(out, evaluateArgsFirst);

        Vector<XPathExpression> v = new Vector<>();
        for (XPathExpression arg : args) {
            v.addElement(arg);
        }
        ExtUtil.write(out, new ExtWrapListPoly(v));
    }

    @Override
    public final Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        IFunctionHandler handler = evalContext.getFunctionHandlers().get(id);
        if (handler != null) {
            return XPathCustomFunc.evalCustomFunction(handler, evaluatedArgs, evalContext);
        } else {
            return evalBody(model, evalContext);
        }
    }

    private void evaluateArguments(DataInstance model, EvaluationContext evalContext) {
        if (evaluateArgsFirst) {
            if (evaluatedArgs == null) {
                evaluatedArgs = new Object[args.length];
            }
            for (int i = 0; i < args.length; i++) {
                evaluatedArgs[i] = args[i].eval(model, evalContext);
            }
        }
    }


    protected abstract Object evalBody(DataInstance model, EvaluationContext evalContext);

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

    /**
     * Perform toUpperCase or toLowerCase on given object.
     */
    protected String normalizeCase(Object o, boolean toUpper) {
        String s = toString(o);
        if (toUpper) {
            return s.toUpperCase();
        }
        return s.toLowerCase();
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

    @Override
    public Object pivot(DataInstance model, EvaluationContext evalContext, Vector<Object> pivots, Object sentinal) throws UnpivotableExpressionException {
        //for now we'll assume that all that functions do is return the composition of their components
        Object[] argVals = new Object[args.length];

        //Identify whether this function is an identity: IE: can reflect back the pivot sentinal with no modification
        String[] identities = new String[]{"string-length"};
        boolean id = false;
        for (String identity : identities) {
            if (identity.equals(id)) {
                id = true;
            }
        }

        //get each argument's pivot
        for (int i = 0; i < args.length; i++) {
            argVals[i] = args[i].pivot(model, evalContext, pivots, sentinal);
        }

        boolean pivoted = false;
        //evaluate the pivots
        for (Object argVal : argVals) {
            if (argVal == null) {
                //one of our arguments contained pivots,
                pivoted = true;
            } else if (sentinal.equals(argVal)) {
                //one of our arguments is the sentinal, return the sentinal if possible
                if (id) {
                    return sentinal;
                } else {
                    //This function modifies the sentinal in a way that makes it impossible to capture
                    //the pivot.
                    throw new UnpivotableExpressionException();
                }
            }
        }

        if (pivoted) {
            if (id) {
                return null;
            } else {
                //This function modifies the sentinal in a way that makes it impossible to capture
                //the pivot.
                throw new UnpivotableExpressionException();
            }
        }

        //TODO: Inner eval here with eval'd args to improve speed
        return eval(model, evalContext);
    }

    protected void validateArgCount() throws XPathSyntaxException {
        if (expectedArgCount != args.length) {
            throw new XPathArityException(id, expectedArgCount, args.length);
        }
    }
}
