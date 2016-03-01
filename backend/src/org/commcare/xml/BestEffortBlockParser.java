package org.commcare.xml;

import org.commcare.data.xml.TransactionParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Hashtable;

/**
 * This parser is for scanning through a block making a best-effort to identify a few
 * nodes inside. Valuable for semi-structured data.
 *
 * Note: Doesn't process attributes usefully yet.
 *
 * @author ctsims
 */
public abstract class BestEffortBlockParser extends TransactionParser<Hashtable<String, String>> {

    final String[] elements;

    public BestEffortBlockParser(KXmlParser parser, String name, String namespace, String[] elements) {
        super(parser);
        this.elements = elements;
    }

    public abstract void commit(Hashtable<String, String> discovered) throws IOException;

    public Hashtable<String, String> parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        String name = parser.getName();
        Hashtable<String, String> ret = new Hashtable<String, String>();

        boolean expecting = false;
        String expected = null;
        while (this.nextTagInBlock(name)) {
            if (expecting) {
                if (parser.getEventType() == KXmlParser.TEXT) {
                    ret.put(expected, parser.getText());
                }
                expecting = false;
            }
            if (matches()) {
                expecting = true;
                expected = parser.getName();
            }
        }
        commit(ret);
        return ret;
    }

    private boolean matches() {
        String name = parser.getName();
        for (String elementName : elements) {
            if (elementName.equals(name)) {
                return true;
            }
        }
        return false;
    }

}
