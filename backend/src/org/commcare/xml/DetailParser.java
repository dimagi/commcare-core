/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Text;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class DetailParser extends ElementParser<Detail> {

	public DetailParser(KXmlParser parser) {
		super(parser);
	}

	public Detail parse() throws InvalidStructureException, IOException, XmlPullParserException {
			checkNode("detail");
		
			String id = parser.getAttributeValue(null,"id");
			
			//First fetch the title
			getNextTagInBlock("detail");
			//inside title, should be a text node as the child
			checkNode("title");
			getNextTagInBlock("title");
			Text title = new TextParser(parser).parse();
			
			//Now get the headers and templates.
			Vector<Text> headers = new Vector<Text>();
			Vector<Text> templates = new Vector<Text>();
			Vector<Integer> headerHints = new Vector<Integer>();
			Vector<Integer> templateHints = new Vector<Integer>();
			Vector<String> headerForms = new Vector<String>();
			Vector<String> templateForms = new Vector<String>();
			Hashtable<String, String> variables = new Hashtable<String, String>();
			int defaultSort = -1;
			
			while(nextTagInBlock("detail")) {
				if("variables".equals(parser.getName().toLowerCase())) {
					while(nextTagInBlock("variables")) {
						variables.put(parser.getName(), parser.getAttributeValue(null, "function"));
					}
					continue;
				}
				
				checkNode("field");
				//Get the fields
				String sortDefault = parser.getAttributeValue(null, "sort");
				if(sortDefault != null && sortDefault.equals("default")) {
					defaultSort = headerForms.size();
				}
				if(nextTagInBlock("field")) {
					//Header
					checkNode("header");
					
					headerHints.addElement(new Integer(getWidth()));
					
					String form = parser.getAttributeValue(null, "form");
					headerForms.addElement(form == null ? "" : form);
					
					parser.nextTag();
					checkNode("text");
					Text header = new TextParser(parser).parse();
					headers.addElement(header);
				}
				if(nextTagInBlock("field")) {
					//Template
					checkNode("template");
					
					templateHints.addElement(new Integer(getWidth()));
					
					String form = parser.getAttributeValue(null, "form");
					templateForms.addElement(form == null ? "" : form);
					
					parser.nextTag();
					checkNode("text");
					Text template = new TextParser(parser).parse();
					templates.addElement(template);
				}
			}
		
		
		
		Detail d = new Detail(id, title, headers, templates, toIntArray(headerHints), toIntArray(templateHints), toStringArray(headerForms), toStringArray(templateForms), defaultSort, variables);
		return d;
	}
	
	private int getWidth() throws InvalidStructureException {
		String width = parser.getAttributeValue(null,"width");
		if(width == null) { return -1; };
		
		//Remove the trailing % sign if any
		if(width.indexOf("%") != -1) {
			width = width.substring(0,width.indexOf("%"));
		}
		return this.parseInt(width);
	}
	
	private int[] toIntArray(Vector<Integer> vector) {
		int[] ret = new int[vector.size()];
		for(int i = 0; i < ret.length ; ++i) {
			ret[i] = vector.elementAt(i).intValue();
		}
		return ret;
	}
	
	private String[] toStringArray(Vector<String> vector) {
		String[] ret = new String[vector.size()];
		for(int i = 0; i < ret.length ; ++i) {
			if(vector.elementAt(i).equals("")) {
				ret[i] = null;
			} else {
				ret[i] = vector.elementAt(i);
			}
		}
		return ret;
	}

}
