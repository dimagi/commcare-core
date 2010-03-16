/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

/**
 * @author ctsims
 *
 */
public class Text implements Externalizable {
	private int type;
	private String argument;
	
	//Will this maintain order? I don't think so....
	private Hashtable<String, Text> arguments;
	
	public static final int TEXT_TYPE_FLAT = 1;
	public static final int TEXT_TYPE_LOCALE = 2;
	public static final int TEXT_TYPE_XPATH = 4;
	public static final int TEXT_TYPE_COMPOSITE = 8;
	
	/**
	 * For Serialization only;
	 */
	public Text() {
		
	}
	
	private static Text TextFactory() {
		Text t = new Text();
		t.type = -1;
		t.argument = "";
		t.arguments = new Hashtable<String, Text>();
		return t;
	}
	
	public static Text LocaleText(String id) {
		Text t = TextFactory();
		t.argument = id;
		t.type = TEXT_TYPE_LOCALE;
		return t;
	}
	
	public static Text LocaleText(Text localeText) {
		Text t = TextFactory();
		t.arguments = new Hashtable<String, Text>();
		t.arguments.put("id",localeText);
		t.argument = "";
		t.type = TEXT_TYPE_LOCALE;
		return t;
	}
	
	public static Text PlainText(String text) {
		Text t = TextFactory();
		t.argument = text;
		t.type = TEXT_TYPE_FLAT;
		return t;
	}
	
	public static Text XPathText(String function, Hashtable<String, Text> arguments) throws XPathSyntaxException {
		Text t = TextFactory();
		t.argument = function;
		//Test parse real fast to make sure it's valid text.
		XPathExpression expression = XPathParseTool.parseXPath("string(" + t.argument + ")");
		t.arguments = arguments;
		t.type = TEXT_TYPE_XPATH;
		return t;
	}
	
	public static Text CompositeText(Vector<Text> text) {
		Text t = TextFactory();
		int i = 0;
		for(Text txt : text) {
			//TODO: Probably a more efficient way to do this...
			t.arguments.put(Integer.toHexString(i), txt);
		}
		t.type = TEXT_TYPE_COMPOSITE;
		return t;
	}
	
	
	
	public String evaluate() {
		return evaluate(null);
	}
	
	public String evaluate(FormInstance context) {
		switch(type) {
		case TEXT_TYPE_FLAT:
			return argument;
		case TEXT_TYPE_LOCALE:
			String id = argument;
			if(argument.equals("")) {
				id = arguments.get("id").evaluate(context);
			}
			return Localization.get(id);
		case TEXT_TYPE_XPATH:
			try {
					//Do an XPath cast to a string as part of the operation.
					XPathExpression expression = XPathParseTool.parseXPath("string(" + argument + ")");
					EvaluationContext temp = new EvaluationContext(new EvaluationContext(), context.getRoot().getRef());
					
					temp.addFunctionHandler(new IFunctionHandler() {

						public Object eval(Object[] args) {
							String type = (String)args[1];
							int format = DateUtils.FORMAT_HUMAN_READABLE_SHORT;
							if(type.equals("short")) {
								format = DateUtils.FORMAT_HUMAN_READABLE_SHORT;
							} else if(type.equals("long")){
								format = DateUtils.FORMAT_ISO8601;
							}
							return DateUtils.formatDate((Date)args[0], format);
						}

						public String getName() {
							return "format_date";
						}

						public Vector getPrototypes() {
							Vector format = new Vector();
							Class[] prototypes = new Class[] {
									Date.class,
									String.class
							};
							format.addElement(prototypes);
							return format;
						}

						public boolean rawArgs() { return false; }

						public boolean realTime() { return false; }
						
					});
					
					
					for(Enumeration en = arguments.keys(); en.hasMoreElements() ;) {
						String key = (String)en.nextElement();
						String value = arguments.get(key).evaluate(context);
						temp.setVariable(key,value);
					}
					
					return (String)expression.eval(context,temp);
				} catch (XPathSyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			//For testing;
			return argument;
		case TEXT_TYPE_COMPOSITE:
			String ret = "";
			for(Enumeration en = arguments.elements() ; en.hasMoreElements() ;) {
				ret += ((Text)en.nextElement()).evaluate() +"::";
			}
			return ret;
		default:
			return argument;
		}
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		type = ExtUtil.readInt(in);
		argument = ExtUtil.readString(in);
		arguments = (Hashtable<String, Text>)ExtUtil.read(in, new ExtWrapMap(String.class, Text.class), pf);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out,type);
		ExtUtil.writeString(out,argument);
		ExtUtil.write(out, new ExtWrapMap(arguments));
	}
}
