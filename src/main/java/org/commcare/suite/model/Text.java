package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.XPathAnalyzable;
import org.javarosa.xpath.analysis.XPathAnalyzer;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import io.reactivex.Single;

/**
 * <p>Text objects are a model for holding strings which
 * will be displayed to users. Text's can be defined
 * in a number of ways, static Strings, localized values,
 * even xpath expressions. They are dynamically evaluated
 * at runtime in order to allow for CommCare apps to flexibly
 * provide rich information from a number of sources.</p>
 *
 * <p>There are 4 types of Text sources which can be defined
 * <ul>
 * <li>Raw Text</li>
 * <li>Localized String</li>
 * <li>XPath Expression</li>
 * <li>Compound Text</li>
 * </ul>
 * </p>
 *
 * @author ctsims
 */
public class Text implements Externalizable, DetailTemplate, XPathAnalyzable {
    private int type;
    private String argument;

    //Will this maintain order? I don't think so....
    private Hashtable<String, Text> arguments;

    private XPathExpression cacheParse;

    public static final int TEXT_TYPE_FLAT = 1;
    public static final int TEXT_TYPE_LOCALE = 2;
    public static final int TEXT_TYPE_XPATH = 4;
    public static final int TEXT_TYPE_COMPOSITE = 8;

    /**
     * For Serialization only;
     */
    public Text() {

    }

    /**
     * @return An empty text object
     */
    private static Text TextFactory() {
        Text t = new Text();
        t.type = -1;
        t.argument = "";
        t.arguments = new Hashtable<>();
        return t;
    }

    /**
     * @param id The locale key.
     * @return A Text object that evaluates to the
     * localized value of the ID provided.
     */
    public static Text LocaleText(String id) {
        return LocaleText(id, null);
    }

    /**
     * @param id The locale key.
     * @param arguments arguments to the localizer
     * @return A Text object that evaluates to the
     * localized value of the ID provided.
     */
    public static Text LocaleText(String id, Hashtable<String, Text> arguments) {
        Text t = TextFactory();
        t.argument = id;
        t.type = TEXT_TYPE_LOCALE;
        t.arguments = arguments;
        return t;
    }


    /**
     * @param localeText A Text object which evaluates
     *                   to a locale key.
     * @return A Text object that evaluates to the
     * localized value of the id returned by evaluating
     * localeText
     */
    public static Text LocaleText(Text localeText) {
        Hashtable<String, Text> arguments = new Hashtable<>();
        arguments.put("id", localeText);
        return LocaleText(arguments);
    }

    /**
     * @return A Text object that evaluates to the
     * localized value of the id returned by evaluating
     * localeText
     */
    public static Text LocaleText(Hashtable<String, Text> arguments) {
        Text t = TextFactory();

        //ensure there is an id text argument
        if (!arguments.containsKey("id")) {
            throw new RuntimeException("Locale text constructor requires 'id' key in arguments");
        }

        t.arguments = arguments;
        t.argument = "";
        t.type = TEXT_TYPE_LOCALE;
        return t;
    }

    /**
     * @param text A text string.
     * @return A Text object that evaluates to the
     * string provided.
     */
    public static Text PlainText(String text) {
        Text t = TextFactory();
        t.argument = text;
        t.type = TEXT_TYPE_FLAT;
        return t;
    }

    /**
     * @param function  A valid XPath function.
     * @param arguments A key/value set defining arguments
     *                  which, when evaluated, will provide a value for variables
     *                  in the provided function.
     * @return A Text object that evaluates to the
     * resulting value of the xpath expression defined
     * by function when presented with a compatible data
     * model.
     * @throws XPathSyntaxException If the provided xpath function does
     *                              not have valid syntax.
     */
    public static Text XPathText(String function, Hashtable<String, Text> arguments) throws XPathSyntaxException {
        Text t = TextFactory();
        t.argument = function;
        //Test parse real fast to make sure it's valid text.
        XPathExpression expression = XPathParseTool.parseXPath("string(" + t.argument + ")");
        t.arguments = arguments;
        t.type = TEXT_TYPE_XPATH;
        return t;
    }

    /**
     * @param text A vector of Text objects.
     * @return A Text object that evaluates to the
     * value of each member of the text vector.
     */
    public static Text CompositeText(Vector<Text> text) {
        Text t = TextFactory();
        int i = 0;
        for (Text txt : text) {
            //TODO: Probably a more efficient way to do this...
            t.arguments.put(Integer.toHexString(i), txt);
            i++;
        }
        t.type = TEXT_TYPE_COMPOSITE;
        return t;
    }


    /**
     * @return The evaluated string value for this Text object. Note
     * that if this string is expecting a model in order to evaluate
     * (like an XPath text), this will likely fail.
     */
    public String evaluate() {
        return evaluate(null);
    }

    /**
     * @param context A data model which is compatible with any
     *                xpath functions in the underlying Text
     * @return The evaluated string value for this Text object.
     */
    @Override
    public String evaluate(EvaluationContext context) {
        switch (type) {
            case TEXT_TYPE_FLAT:
                return argument;
            case TEXT_TYPE_LOCALE:
                String id = argument;
                if (argument.equals("")) {
                    id = arguments.get("id").evaluate(context);
                }

                String[] params = generateOrderedParameterListForLocalization(arguments, context);

                return Localization.get(id, params);
            case TEXT_TYPE_XPATH:
                try {
                    ensureCacheIsParsed();

                    //We need an EvaluatonContext in a specific sense in order to evaluate certain components
                    //like Instance references or relative references to some models, but it's valid to use
                    //XPath expressions for other things like Dates, or simply manipulating other variables,
                    //so if we don't have one, we can make one that doesn't reference any data specifically
                    EvaluationContext temp;
                    if (context == null) {
                        temp = new EvaluationContext(null);
                    } else {
                        temp = new EvaluationContext(context, context.getContextRef());
                    }

                    temp.addFunctionHandler(new IFunctionHandler() {

                        @Override
                        public Object eval(Object[] args, EvaluationContext ec) {
                            Object o = FunctionUtils.toDate(args[0]);
                            if (!(o instanceof Date)) {
                                //return null, date is null.
                                return "";
                            }

                            String type = (String) args[1];
                            int format = DateUtils.FORMAT_HUMAN_READABLE_SHORT;
                            if (type.equals("short")) {
                                format = DateUtils.FORMAT_HUMAN_READABLE_SHORT;
                            } else if (type.equals("long")) {
                                format = DateUtils.FORMAT_ISO8601;
                            }
                            return DateUtils.formatDate((Date) o, format);
                        }

                        @Override
                        public String getName() {
                            return "format_date";
                        }

                        @Override
                        public Vector getPrototypes() {
                            Vector format = new Vector();
                            Class[] prototypes = new Class[]{
                                    Date.class,
                                    String.class
                            };
                            format.addElement(prototypes);
                            return format;
                        }

                        @Override
                        public boolean rawArgs() {
                            return false;
                        }

                    });

                    temp.addFunctionHandler(new IFunctionHandler() {

                        @Override
                        public Object eval(Object[] args, EvaluationContext ec) {
                            Calendar c = Calendar.getInstance();
                            c.setTime(new Date());
                            return String.valueOf(c.get(Calendar.DAY_OF_WEEK));
                        }

                        @Override
                        public String getName() {
                            return "dow";
                        }

                        @Override
                        public Vector getPrototypes() {
                            Vector format = new Vector();
                            Class[] prototypes = new Class[]{};
                            format.addElement(prototypes);
                            return format;
                        }

                        @Override
                        public boolean rawArgs() {
                            return false;
                        }
                    });


                    for (Enumeration en = arguments.keys(); en.hasMoreElements(); ) {
                        String key = (String) en.nextElement();
                        String value = arguments.get(key).evaluate(context);
                        temp.setVariable(key, value);
                    }

                    return (String) cacheParse.eval(temp.getMainInstance(), temp);
                } catch (XPathSyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (XPathException e) {
                    e.setSource(argument);
                    throw e;
                }
                //For testing;
                return argument;
            case TEXT_TYPE_COMPOSITE:
                String ret = "";
                for (int i = 0; i < arguments.size(); ++i) {
                    Text item = arguments.get(String.valueOf(i));
                    ret += item.evaluate(context) + "";
                }
                return ret;
            default:
                return argument;
        }
    }

    private String[] generateOrderedParameterListForLocalization(Hashtable<String, Text> arguments,
                                                                 EvaluationContext context) {
        if(arguments == null) {
            return new String[0];
        }

        List<String> keys = getOrderedKeys(arguments);

        if(keys.size() == 0) {
            return new String[0];
        }

        String[] parameters = new String[keys.size()];
        for(int i = 0; i < keys.size(); ++i) {
            parameters[i] = arguments.get(keys.get(i)).evaluate(context);
        }

        return parameters;
    }

    private List<String> getOrderedKeys(Hashtable<String, Text> arguments) {
        List<String> keys = new ArrayList<>();

        for(String key : arguments.keySet()) {
            if(key.equals("id")) {
                continue;
            }
            keys.add(key);
        }

        //This code uses a hacky shortcut to need to prevent type coercing the keys into integers,
        //and just sorts them alphanumerically, which will fail if there are more than 10 keys.
        //This check should keep us honest should we ever need to fix that.
        if(keys.size() > 10) {
            throw new RuntimeException("Too many arguments - Text params only support 10");
        }

        Collections.sort(keys, (s1, s2) -> s1.compareTo(s2));
        return keys;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        type = ExtUtil.readInt(in);
        argument = ExtUtil.readString(in);
        arguments = (Hashtable<String, Text>)ExtUtil.read(in, new ExtWrapMap(String.class, Text.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, type);
        ExtUtil.writeString(out, argument);
        ExtUtil.write(out, new ExtWrapMap(arguments));
    }

    public String getArgument() {
        return argument;
    }

    @Override
    public void applyAndPropagateAnalyzer(XPathAnalyzer analyzer) throws AnalysisInvalidException {
        if (analyzer.shortCircuit()) {
            return;
        }
        if (this.type == Text.TEXT_TYPE_XPATH) {
            try {
                ensureCacheIsParsed();
            } catch (XPathSyntaxException e) {
                throw AnalysisInvalidException.INSTANCE_TEXT_PARSE_FAILURE;
            }
            cacheParse.applyAndPropagateAnalyzer(analyzer);
        } else if (arguments != null) {
            for (Text t : arguments.values()) {
                t.applyAndPropagateAnalyzer(analyzer);
            }
        }
    }

    public void ensureCacheIsParsed() throws XPathSyntaxException {
        if (cacheParse == null) {
            //Do an XPath cast to a string as part of the operation.
            cacheParse = XPathParseTool.parseXPath("string(" + argument + ")");
        }
    }

    /**
     * Get back a single disposable which can be executed to calculate the value of this Text.
     *
     * The query evaluation will be abandoned if disposed.
     */
    public Single<String> getDisposableSingleForEvaluation(EvaluationContext ec) {
        final EvaluationContext abandonableContext = ec.spawnWithCleanLifecycle();

        final Thread[] toCancel = new Thread[1];
        return Single.fromCallable(() -> {
            toCancel[0] = Thread.currentThread();
            return evaluate(abandonableContext);
        }).doOnDispose(() -> {
            if(toCancel[0] != null) {
                toCancel[0].interrupt();
                toCancel[0] = null;
            }
        });
    }
}
