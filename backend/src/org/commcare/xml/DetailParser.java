/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Vector;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Text;
import org.commcare.xml.util.InvalidStructureException;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.parser.XPathSyntaxException;
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
			OrderedHashtable<String, String> variables = new OrderedHashtable<String, String>();
			int defaultSort = -1;
			
			while(nextTagInBlock("detail")) {
				if("variables".equals(parser.getName().toLowerCase())) {
					while(nextTagInBlock("variables")) {
						String function = parser.getAttributeValue(null, "function");
						if(function == null) { throw new InvalidStructureException("No function in variable declaration for variable " + parser.getName(), parser); }
						try {
							XPathParseTool.parseXPath(function);
						} catch (XPathSyntaxException e) {
							e.printStackTrace();
							throw new InvalidStructureException("Invalid XPath function " + function +". " + e.getMessage(), parser);
						}
						variables.put(parser.getName(), function);
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
