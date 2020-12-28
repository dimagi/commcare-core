package org.commcare.xml;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.QueryPrompt;
import org.javarosa.core.model.ItemsetBinding;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xform.parse.ItemSetParsingUtils;
import org.javarosa.xform.util.XFormSerializer;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathConditional;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import static org.javarosa.xform.parse.XFormParser.LABEL_ELEMENT;

public class QueryPromptParser extends CommCareElementParser<QueryPrompt> {

    private static final String NAME_PROMPT = "prompt";
    private static final String NAME_DISPLAY = "display";
    private static final String NAME_ITEMSET = "itemset";
    private static final String ATTR_APPEARANCE = "appearance";
    private static final String ATTR_KEY = "key";
    private static final String ATTR_INPUT = "input";
    private static final String ATTR_NODESET = "nodeset";
    private static final String NAME_LABEL = "label";
    private static final String NAME_VALUE = "value";
    private static final String NAME_SORT = "sort";
    private static final String ATTR_REF = "ref";

    public QueryPromptParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public QueryPrompt parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        String appearance = parser.getAttributeValue(null, ATTR_APPEARANCE);
        String key = parser.getAttributeValue(null, ATTR_KEY);
        String input = parser.getAttributeValue(null, ATTR_INPUT);
        DisplayUnit display = null;
        ItemsetBinding itemsetBinding = null;
        while (nextTagInBlock(NAME_PROMPT)) {
            if (NAME_DISPLAY.equals(parser.getName().toLowerCase())) {
                display = parseDisplayBlock();
            } else if (NAME_ITEMSET.equals(parser.getName().toLowerCase())) {
                itemsetBinding = parseItemset();
            }
        }
        return new QueryPrompt(key, appearance, input, display, itemsetBinding);
    }

    private ItemsetBinding parseItemset() throws IOException, XmlPullParserException, InvalidStructureException {
        ItemsetBinding itemset = new ItemsetBinding();
        itemset.contextRef = TreeReference.rootRef();
        String nodesetStr = parser.getAttributeValue(null, ATTR_NODESET);
        ItemSetParsingUtils.setNodeset(itemset, nodesetStr, NAME_ITEMSET);
        while (nextTagInBlock(NAME_ITEMSET)) {
            if (NAME_LABEL.equals(parser.getName())) {
                ItemSetParsingUtils.setLabel(itemset, parser.getAttributeValue(null, ATTR_REF));
            } else if (NAME_VALUE.equals(parser.getName())) {
                ItemSetParsingUtils.setValue(itemset, parser.getAttributeValue(null, ATTR_REF));
            } else if (NAME_SORT.equals(parser.getName())) {
                ItemSetParsingUtils.setSort(itemset, parser.getAttributeValue(null, ATTR_REF));
            }
        }
        return itemset;
    }
}
