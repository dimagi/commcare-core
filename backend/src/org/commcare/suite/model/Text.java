/**
 * 
 */
package org.commcare.suite.model;

import java.util.Hashtable;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.locale.Localization;

/**
 * @author ctsims
 *
 */
public class Text {
	private Text[] components;
	private int type;
	private String argument;
	
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
	
	public static Text LocaleText(String id) {
		Text t = new Text();
		t.argument = id;
		t.type = TEXT_TYPE_LOCALE;
		return t;
	}
	
	public static Text LocaleText(Text localeText) {
		Text t = new Text();
		t.arguments = new Hashtable<String, Text>();
		t.arguments.put("id",localeText);
		t.argument = null;
		t.type = TEXT_TYPE_LOCALE;
		return t;
	}
	
	public static Text PlainText(String text) {
		Text t = new Text();
		t.argument = text;
		t.type = TEXT_TYPE_FLAT;
		return t;
	}
	
	public static Text XPathText(String function, Hashtable<String, Text> arguments) {
		Text t = new Text();
		t.argument = function;
		t.arguments = arguments;
		t.type = TEXT_TYPE_XPATH;
		return t;
	}
	
	public static Text CompositeText(Text[] text) {
		Text t = new Text();
		t.components = text;
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
			return id;
			//return Localization.get(id);
		case TEXT_TYPE_XPATH:
			//For testing;
			return argument;
		case TEXT_TYPE_COMPOSITE:
			String ret = "";
			for(int i = 0 ; i < components.length ; ++i ) {
				ret += components[i].evaluate() +"::";
			}
			return ret;
		default:
			return argument;
		}
			
	}
}
