package org.commcare.xml;

import org.commcare.suite.model.ComputedDatum;
import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.FormIdDatum;
import org.commcare.suite.model.QueryPrompt;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.SessionDatum;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

/**
 * @author ctsims
 */
public class SessionDatumParser extends CommCareElementParser<SessionDatum> {

    public SessionDatumParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public SessionDatum parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        if ("query".equals(parser.getName())) {
            return parseRemoteQueryDatum();
        }

        if ((!"datum".equals(this.parser.getName())) && !("form".equals(this.parser.getName()))) {
            throw new InvalidStructureException("Expected <datum> or <form> data in <session> block, instead found " + this.parser.getName() + ">", this.parser);
        }

        String id = parser.getAttributeValue(null, "id");

        String calculate = parser.getAttributeValue(null, "function");

        SessionDatum datum;
        if (calculate == null) {
            String nodeset = parser.getAttributeValue(null, "nodeset");
            String shortDetail = parser.getAttributeValue(null, "detail-select");
            String longDetail = parser.getAttributeValue(null, "detail-confirm");
            String inlineDetail = parser.getAttributeValue(null, "detail-inline");
            String persistentDetail = parser.getAttributeValue(null, "detail-persistent");
            String value = parser.getAttributeValue(null, "value");
            String autoselect = parser.getAttributeValue(null, "autoselect");

            if (nodeset == null) {
                throw new InvalidStructureException("Expected @nodeset in " + id + " <datum> definition", this.parser);
            }

            datum = new EntityDatum(id, nodeset, shortDetail, longDetail, inlineDetail,
                    persistentDetail, value, autoselect);
        } else {
            if ("form".equals(this.parser.getName())) {
                datum = new FormIdDatum(calculate);
            } else {
                datum = new ComputedDatum(id, calculate);
            }
        }

        while (parser.next() == KXmlParser.TEXT) ;

        return datum;
    }

    private RemoteQueryDatum parseRemoteQueryDatum()
            throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        Hashtable<String, XPathExpression> hiddenQueryValues =
                new Hashtable<>();
        OrderedHashtable<String, QueryPrompt> userQueryPrompts =
                new OrderedHashtable<>();
        this.checkNode("query");

        // The 'template' argument specifies whether the query result should follow a specific xml structure.
        // Currently only 'case' is supported; asserting the casedb xml structure
        String xpathTemplateType = parser.getAttributeValue(null, "template");
        boolean useCaseTemplate = "case".equals(xpathTemplateType);

        String queryUrlString = parser.getAttributeValue(null, "url");
        String queryResultStorageInstance = parser.getAttributeValue(null, "storage-instance");
        if (queryUrlString == null || queryResultStorageInstance == null) {
            String errorMsg = "<query> element missing 'url' or 'storage-instance' attribute";
            throw new InvalidStructureException(errorMsg, parser);
        }
        URL queryUrl;
        try {
            queryUrl = new URL(queryUrlString);
        } catch (MalformedURLException e) {
            String errorMsg =
                    "<query> element has invalid 'url' attribute (" + queryUrlString + ").";
            throw new InvalidStructureException(errorMsg, parser);
        }

        while (nextTagInBlock("query")) {
            String tagName = parser.getName();
            if ("data".equals(tagName)) {
                String key = parser.getAttributeValue(null, "key");
                String ref = parser.getAttributeValue(null, "ref");
                try {
                    hiddenQueryValues.put(key, XPathParseTool.parseXPath(ref));
                } catch (XPathSyntaxException e) {
                    String errorMessage = "'ref' value is not a valid xpath expression: " + ref;
                    throw new InvalidStructureException(errorMessage, this.parser);
                }
            } else if ("prompt".equals(tagName)) {
                String key = parser.getAttributeValue(null, "key");
                userQueryPrompts.put(key, new QueryPromptParser(parser).parse());
            }
        }

        return new RemoteQueryDatum(queryUrl, queryResultStorageInstance,
                hiddenQueryValues, userQueryPrompts, useCaseTemplate);
    }
}
