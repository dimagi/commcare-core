package org.commcare.xml;

import org.commcare.suite.model.Text;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

public class TextParser extends ElementParser<Text> {

    public TextParser(KXmlParser parser) {
        super(parser);
    }

    public Text parse() throws InvalidStructureException, IOException, XmlPullParserException {
        Vector<Text> texts = new Vector<Text>();

        checkNode("text");
        int entryLevel = parser.getDepth();
        try {
            parser.next();
        } catch (XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        while (parser.getDepth() > entryLevel || parser.getEventType() == KXmlParser.TEXT) {
            Text t = parseBody();
            if (t != null) {
                texts.addElement(t);
            }
        }
        if (texts.size() == 1) {
            return texts.elementAt(0);
        } else {
            return Text.CompositeText(texts);
        }
    }

    private Text parseBody() throws InvalidStructureException, IOException, XmlPullParserException {
        //TODO: Should prevent compositing text and xpath/locales
        Vector<Text> texts = new Vector<Text>();

        int eventType = parser.getEventType();
        String text = "";
        do {
            if (eventType == KXmlParser.START_TAG) {
                //If we were parsing text, commit that up first.
                if (!text.trim().equals("")) {
                    Text t = Text.PlainText(text);
                    texts.addElement(t);
                    text = "";
                }

                //now parse out the next tag.
                if (parser.getName().toLowerCase().equals("xpath")) {
                    Text xpathText = parseXPath();
                    texts.addElement(xpathText);
                } else if (parser.getName().toLowerCase().equals("locale")) {
                    Text localeText = parseLocale();
                    texts.addElement(localeText);
                }
            } else if (eventType == KXmlParser.TEXT) {
                text += parser.getText().trim();
            }

            //We shouldn't really ever get here as far as things are currently set up
            eventType = parser.next();
            //How do we get out of here? Depth?
        } while (eventType != KXmlParser.END_TAG);

        if (!text.trim().equals("")) {
            Text t = Text.PlainText(text);
            texts.addElement(t);
        }
        if (texts.size() == 0) {
            return null;
        }

        if (texts.size() == 1) {
            return texts.elementAt(0);
        } else {
            return Text.CompositeText(texts);
        }
    }

    private Text parseLocale() throws InvalidStructureException, IOException, XmlPullParserException {
        checkNode("locale");
        String id = parser.getAttributeValue(null, "id");
        if (id != null) {
            return Text.LocaleText(id);
        } else {
            //Get ID Node, throw exception if there isn't a tag
            getNextTagInBlock("locale");
            checkNode("id");
            Text idText = new TextParser(parser).parseBody();
            return Text.LocaleText(idText);
        }
    }

    private Text parseXPath() throws InvalidStructureException, IOException, XmlPullParserException {
        checkNode("xpath");
        String function = parser.getAttributeValue(null, "function");
        Hashtable<String, Text> arguments = new Hashtable<String, Text>();

        //Now get all of the variables which might be used
        while (nextTagInBlock("xpath")) {
            checkNode("variable");
            String name = parser.getAttributeValue(null, "name");
            Text variableText = new TextParser(parser).parseBody();
            arguments.put(name, variableText);
        }
        try {
            return Text.XPathText(function, arguments);
        } catch (XPathSyntaxException e) {
            e.printStackTrace();
            throw new InvalidStructureException("Invalid XPath Expression : " + function + ". Parse error: " + e.getMessage(), parser);
        }
    }
}
