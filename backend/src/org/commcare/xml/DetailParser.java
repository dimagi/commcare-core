/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Stack;
import java.util.Vector;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Text;
import org.commcare.xml.util.InvalidStructureException;
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

	public Detail parse() throws InvalidStructureException {
		if(!parser.getName().toLowerCase().equals("detail")) {
			throw new InvalidStructureException();
		}
		
		try {
			String id = parser.getAttributeValue(null,"id");
			parser.nextTag();
			//model first
			FormInstance model = parseModel();
			
			Vector<Text> headers = new Vector<Text>();;
			Vector<Text> templates = new Vector<Text>();
			
			while(nextTagInBlock("detail")) {
				checkNode("field");
				//Get the fields
				if(nextTagInBlock("field")) {
					//Header
					checkNode("header");
					parser.nextTag();
					checkNode("text");
					Text header = new TextParser(parser).parse();
					headers.addElement(header);
				}
				if(nextTagInBlock("field")) {
					//Template
					checkNode("template");
					parser.nextTag();
					checkNode("text");
					Text template = new TextParser(parser).parse();
					templates.addElement(template);
				}
			}
		
		
		Detail d = new Detail(id, model, headers, templates);
		return d;
		
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			throw new InvalidStructureException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new InvalidStructureException();
		}
	}
	
	private FormInstance parseModel() throws InvalidStructureException {
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
