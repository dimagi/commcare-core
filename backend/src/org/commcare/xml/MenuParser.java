/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Vector;

import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Text;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class MenuParser extends ElementParser<Menu> {
	
	public MenuParser(KXmlParser parser) {
		super(parser);
	}

	/* (non-Javadoc)
	 * @see org.commcare.xml.ElementParser#parse()
	 */
	public Menu parse() throws InvalidStructureException, IOException, XmlPullParserException {
		checkNode("menu");

		String id = parser.getAttributeValue(null, "id");
		String root = parser.getAttributeValue(null, "root");
		root = root == null? "root" : root;
		getNextTagInBlock("menu");
		
		
		
		Text name;
		String imageURI = null;;
		String audioURI=  null;
		
		if(parser.getName().equals("text")){
			name = new TextParser(parser).parse();
		}else if(parser.getName().equals("display")){
			Object[] displayArr = parseDisplayBlock();
			//check that we have a commandText;
			if(displayArr[0] == null) throw new InvalidStructureException("Expected Menu Text in Display block",parser);
			else name = (Text)displayArr[0];
			
			imageURI = (String)displayArr[1];
			audioURI = (String)displayArr[2];
		} else {
			throw new InvalidStructureException("Expected either <text> or <display> in menu",parser);
		}

		
		//name = new TextParser(parser).parse();

		Vector<String> commandIds = new Vector<String>();
		while (nextTagInBlock("menu")) {
			checkNode("command");
			commandIds.addElement(parser.getAttributeValue(null, "id"));
		}

		Menu m = new Menu(id, root, name, commandIds, imageURI, audioURI);
		return m;

	}

}
