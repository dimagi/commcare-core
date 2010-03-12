/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
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
	
	public static Text XPathText(String function, Hashtable<String, Text> arguments) {
		Text t = TextFactory();
		t.argument = function;
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
			if(argument == null) {
				id = arguments.get("id").evaluate(context);
			}
			return Localization.get(id);
		case TEXT_TYPE_XPATH:
			try {
					XPathExpression expression = XPathParseTool.parseXPath(argument);
					EvaluationContext temp = new EvaluationContext(new EvaluationContext(), context.getRoot().getRef());
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
