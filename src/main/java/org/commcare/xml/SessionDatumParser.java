package org.commcare.xml;

import org.commcare.cases.util.StringUtils;
import org.commcare.suite.model.ComputedDatum;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.FormIdDatum;
import org.commcare.suite.model.MultiSelectEntityDatum;
import org.commcare.suite.model.QueryData;
import org.commcare.suite.model.QueryPrompt;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.Text;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * @author ctsims
 */
public class SessionDatumParser extends CommCareElementParser<SessionDatum> {

    public SessionDatumParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public SessionDatum parse()
            throws InvalidStructureException, IOException, XmlPullParserException,
            UnfullfilledRequirementsException {
        String name = parser.getName();
        if ("query".equals(name)) {
            return parseRemoteQueryDatum();
        }

        if ((!"datum".equals(name)) && !("form".equals(name)) && !("instance-datum".equals(name))) {
            throw new InvalidStructureException(
                    "Expected one of <instance-datum>, <datum> or <form> data in <session> block,"
                            + " instead found "
                            + this.parser.getName() + ">", this.parser);
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
            String maxSelectValueStr = parser.getAttributeValue(null, "max-select-value");
            int maxSelectValue = -1;
            if (!StringUtils.isEmpty(maxSelectValueStr)) {
                try {
                    maxSelectValue = Integer.parseInt(maxSelectValueStr);
                } catch (NumberFormatException e) {
                    throw new InvalidStructureException(
                            "Invalid value " + maxSelectValueStr
                                    + "for max-select-value. Must be an Integer", this.parser);
                }
            }

            if (nodeset == null) {
                throw new InvalidStructureException(
                        "Expected @nodeset in " + id + " <datum> definition", this.parser);
            }

            if ("instance-datum".equals(name)) {
                datum = new MultiSelectEntityDatum(id, nodeset, shortDetail, longDetail, inlineDetail,
                        persistentDetail, value, autoselect, maxSelectValue);
            } else {
                datum = new EntityDatum(id, nodeset, shortDetail, longDetail, inlineDetail,
                        persistentDetail, value, autoselect);
            }
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
            throws InvalidStructureException, IOException, XmlPullParserException,
            UnfullfilledRequirementsException {
        OrderedHashtable<String, QueryPrompt> userQueryPrompts = new OrderedHashtable<>();
        this.checkNode("query");

        // The 'template' argument specifies whether the query result should follow a specific
        // xml structure.
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

        boolean defaultSearch = "true".equals(parser.getAttributeValue(null, "default_search"));
        Text title = null;

        getNextTagInBlock("title");
        if ("title".equals(parser.getName())) {
            nextTagInBlock();
            title = new TextParser(parser).parse(); 
        }

        ArrayList<QueryData> hiddenQueryValues = new ArrayList<QueryData>();
        while (nextTagInBlock("query")) {
            String tagName = parser.getName();
            if ("data".equals(tagName)) {
                hiddenQueryValues.add(new QueryDataParser(parser).parse());
            } else if ("prompt".equals(tagName)) {
                String key = parser.getAttributeValue(null, "key");
                userQueryPrompts.put(key, new QueryPromptParser(parser).parse());
            }
        }
        return new RemoteQueryDatum(queryUrl, queryResultStorageInstance,
                hiddenQueryValues, userQueryPrompts, useCaseTemplate, defaultSearch, title);
    }
}
