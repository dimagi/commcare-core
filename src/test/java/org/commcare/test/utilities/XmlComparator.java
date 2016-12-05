package org.commcare.test.utilities;

import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

/**
 * @author ctsims
 */
public class XmlComparator {

    public static Document getDocumentFromStream(InputStream is) {
        KXmlParser parser = new KXmlParser();

        Document document = new Document();

        try {
            InputStreamReader reader = new InputStreamReader(is, "UTF-8");
            parser.setInput(reader);
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            document.parse(parser);
            return document;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Takes two DOM documents and compares them to see whether they have semantically identical
     * data. For our purposes semantic equality means:
     * - Both documents have the same elements in the same order
     * - All DOM elements have the same attributes with the same values
     * - All DOM elements with values contain the same value
     *
     * If any of these conditions are not met, a runtime exception containing a user readable message
     * will be thrown, but may be lacking robust context for where the DOM's fail to match.
     */
    public static void isDOMEqual(Document a, Document b) throws RuntimeException {
        isDOMEqualRecursive(a.getRootElement(), b.getRootElement());
        isDOMEqualRecursive(b.getRootElement(), a.getRootElement());
    }

    public static void isDOMEqualRecursive(Element left, Element right) throws RuntimeException {
        if(!left.getName().equals(right.getName())) {
            throw new RuntimeException(String.format("Mismatched element names '%s' and '%s'", left.getName(), right.getName()));
        }

        if(left.getAttributeCount() != right.getAttributeCount()) {
            throw new RuntimeException(String.format("Mismatched attributes for node '%s' ", left.getName()));
        }

        Hashtable<String, String> leftAttr = attrTable(left);
        Hashtable<String, String> rightAttr = attrTable(right);

        for (String key : leftAttr.keySet()) {
            if (!rightAttr.containsKey(key)) {
                throw new RuntimeException(String.format("Mismatched attributes for node '%s' ", left.getName()));
            }

            if (!leftAttr.get(key).equals(rightAttr.get(key))) {
                throw new RuntimeException(String.format("Mismatched attributes for node '%s' ", left.getName()));
            }
        }

        if (left.getChildCount() != right.getChildCount()) {
            throw new RuntimeException(String.format("Mismatched child count (%d,%d) for node '%s' ", left.getChildCount(), right.getChildCount(), left.getName()));
        }

        for (int i = 0; i < left.getChildCount(); ++i) {
            Object l = left.getChild(i);
            Object r = right.getChild(i);

            if (left.getType(i) != right.getType(i)) {
                throw new RuntimeException(String.format("Mismatched children for node '%s' ", left.getName()));
            }

            if(l instanceof Element) {
                isDOMEqualRecursive((Element)l, (Element)r);
            } else if(l instanceof String) {
                if(!l.equals(r)) {
                    throw new RuntimeException(String.format("Mismatched element values '%s' and '%s'", l, r));
                }
            }
        }
    }

    private static Hashtable<String, String> attrTable(Element element) {
        Hashtable<String, String> attr = new Hashtable<>();
        for(int i = 0 ; i < element.getAttributeCount() ; ++i ) {
            attr.put(element.getAttributeName(i), element.getAttributeValue(i));
        }
        return attr;
    }
}

