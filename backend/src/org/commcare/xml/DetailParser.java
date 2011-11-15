/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Filter;
import org.commcare.suite.model.Text;
import org.commcare.xml.util.InvalidStructureException;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
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
			
			getNextTagInBlock("detail");
			
			Filter filter = Filter.EmptyFilter();
			
			if(parser.getName().toLowerCase().equals("filter")) {
				filter = new FilterParser(parser).parse();
				getNextTagInBlock("detail");
			}
			
			//Now the model
			FormInstance model = parseModel();
			
			Hashtable<String, DataInstance> instances = new Hashtable<String, DataInstance>();
			if("instance".equals(parser.getName())) {
				String instanceId = parser.getAttributeValue(null, "id");
				String location = parser.getAttributeValue(null,"src");
				instances.put(instanceId, new ExternalDataInstance(location, instanceId));
			}
			
			//Now get the headers and templates.
			Vector<Text> headers = new Vector<Text>();
			Vector<Text> templates = new Vector<Text>();
			Vector<Integer> headerHints = new Vector<Integer>();
			Vector<Integer> templateHints = new Vector<Integer>();
			Vector<String> headerForms = new Vector<String>();
			Vector<String> templateForms = new Vector<String>();
			int defaultSort = -1;
			
			while(nextTagInBlock("detail")) {
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
		
		
		
		Detail d = new Detail(id, title, model, headers, templates,filter, toIntArray(headerHints), toIntArray(templateHints), toStringArray(headerForms), toStringArray(templateForms), defaultSort, instances);
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
	
	private FormInstance parseModel() throws InvalidStructureException, IOException, XmlPullParserException {
		checkNode("model");
		int startingDepth = parser.getDepth();
		int currentDepth = 1;
		Stack<TreeElement> parents = new Stack<TreeElement>();
		
		//Navigate to the first element
		nextTagInBlock("model");
		TreeElement root = new TreeElement(parser.getName());
		for(int i = 0 ; i < parser.getAttributeCount(); ++i) {
			root.setAttribute(null, parser.getAttributeName(i), parser.getAttributeValue(i));
		}
		parents.push(root);
		
		//Get each child;
		while(nextTagInBlock("model")) {
			
			int relativeDepth = parser.getDepth() - startingDepth - 1;
			
			TreeElement element = new TreeElement(parser.getName());
			for(int i = 0 ; i < parser.getAttributeCount(); ++i) {
				element.setAttribute(null, parser.getAttributeName(i), parser.getAttributeValue(i));
			}

			if(currentDepth == relativeDepth) {
				parents.peek().addChild(element);
			} else if(currentDepth < relativeDepth) {
				parents.peek().addChild(element);
				parents.push(element);
				currentDepth++;
			} else if(currentDepth > relativeDepth) {
				parents.pop();
				parents.peek().addChild(element);
				currentDepth--;
			}
		}
		FormInstance instance = new FormInstance(root);
		return instance;
	}

}
