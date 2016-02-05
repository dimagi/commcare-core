package org.commcare.xml;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.RemoteQuery;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathParseTool;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Hashtable;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class SessionQueryParser extends CommCareElementParser<RemoteQuery> {
    Hashtable<String, TreeReference> hiddenQueryValues = new Hashtable<String, TreeReference>();
    Hashtable<String, DisplayUnit> userQueryPrompts = new Hashtable<String, DisplayUnit>();

    public SessionQueryParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public RemoteQuery parse() throws InvalidStructureException, IOException, XmlPullParserException {
        this.checkNode("query");

        String queryUrl = parser.getAttributeValue(null, "url");
        String queryResultStorageInstance = parser.getAttributeValue(null, "storage-instance");
        if (queryUrl == null || queryResultStorageInstance == null) {
            throw new InvalidStructureException("<query> element missing 'url' or 'storage-instance' attribute", parser);
        }

        while (nextTagInBlock("query")) {
            String tagName = parser.getName();
            if ("data".equals(tagName)) {
                String key = parser.getAttributeValue(null, "key");
                String ref = parser.getAttributeValue(null, "ref");
                hiddenQueryValues.put(key, XPathReference.getPathExpr(ref).getReference());
            } else if ("prompt".equals(tagName)) {
                String key = parser.getAttributeValue(null, "key");
                DisplayUnit display = parseDisplayBlock();
                userQueryPrompts.put(key, display);
            }
        }
        return new RemoteQuery(queryUrl, queryResultStorageInstance, hiddenQueryValues, userQueryPrompts);
    }
}
