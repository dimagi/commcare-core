package org.commcare.xml;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.QueryPrompt;
import org.commcare.suite.model.QueryPromptValidation;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.ItemsetBinding;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xform.parse.ItemSetParsingUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class QueryPromptParser extends CommCareElementParser<QueryPrompt> {

    private static final String NAME_PROMPT = "prompt";
    private static final String NAME_DISPLAY = "display";
    private static final String NAME_ITEMSET = "itemset";
    private static final String NAME_VALIDATION = "validation";
    private static final String ATTR_APPEARANCE = "appearance";
    private static final String ATTR_KEY = "key";
    private static final String ATTR_INPUT = "input";
    private static final String ATTR_RECEIVE = "receive";
    private static final String ATTR_HIDDEN = "hidden";
    private static final String ATTR_NODESET = "nodeset";
    private static final String ATTR_DEFAULT = "default";
    private static final String NAME_LABEL = "label";
    private static final String NAME_VALUE = "value";
    private static final String NAME_SORT = "sort";
    private static final String ATTR_REF = "ref";
    private static final String ATTR_ALLOW_BLANK_VALUE = "allow_blank_value";
    private static final String ATTR_EXCLUDE = "exclude";
    private static final String ATTR_REQUIRED = "required";
    private static final String ATTR_VALIDATION_TEST = "test";
    private static final String NAME_TEXT = "text";

    public QueryPromptParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public QueryPrompt parse() throws InvalidStructureException, IOException, XmlPullParserException,
            UnfullfilledRequirementsException {
        String appearance = parser.getAttributeValue(null, ATTR_APPEARANCE);
        String key = parser.getAttributeValue(null, ATTR_KEY);
        String input = parser.getAttributeValue(null, ATTR_INPUT);
        String receive = parser.getAttributeValue(null, ATTR_RECEIVE);
        String hidden = parser.getAttributeValue(null, ATTR_HIDDEN);
        boolean allowBlankValue = "true".equals(parser.getAttributeValue(null, ATTR_ALLOW_BLANK_VALUE));
        DisplayUnit display = null;
        ItemsetBinding itemsetBinding = null;
        String defaultValueString = parser.getAttributeValue(null, ATTR_DEFAULT);
        XPathExpression defaultValue = xpathPropertyValue(defaultValueString);
        String excludeValueString = parser.getAttributeValue(null, ATTR_EXCLUDE);
        XPathExpression exclude = xpathPropertyValue(excludeValueString);
        XPathExpression required = xpathPropertyValue(parser.getAttributeValue(null, ATTR_REQUIRED));

        QueryPromptValidation validation = null;
        while (nextTagInBlock(NAME_PROMPT)) {
            if (NAME_DISPLAY.equalsIgnoreCase(parser.getName())) {
                display = parseDisplayBlock();
            } else if (NAME_ITEMSET.equalsIgnoreCase(parser.getName())) {
                itemsetBinding = parseItemset();
            } else if (NAME_VALIDATION.equalsIgnoreCase(parser.getName())) {
                validation = parseValidationBlock(key);
            }
        }
        return new QueryPrompt(key, appearance, input, receive, hidden, display,
                itemsetBinding, defaultValue, allowBlankValue, exclude, required, validation);
    }

    private QueryPromptValidation parseValidationBlock(String key)
            throws InvalidStructureException, XmlPullParserException, IOException {
        String testStr = parser.getAttributeValue(null, ATTR_VALIDATION_TEST);
        if (testStr == null) {
            throw new InvalidStructureException("No test condition defined in validation for prompt " + key);
        }
        XPathExpression test = xpathPropertyValue(testStr);
        Text message = null;
        while (nextTagInBlock(NAME_VALIDATION)) {
            if (parser.getName().equals(NAME_TEXT)) {
                message = new TextParser(parser).parse();
            } else {
                throw new InvalidStructureException(
                        "Unrecognised node " + parser.getName() + "in validation for prompt " + key);
            }
        }
        if (message == null) {
            throw new InvalidStructureException(
                    "No validation message defined in the validation block for prompt " + key);
        }
        return new QueryPromptValidation(test, message);
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

    public XPathExpression xpathPropertyValue(String xpath) throws InvalidStructureException {
        XPathExpression propertyValue = null;
        if (xpath != null) {
            try {
                propertyValue = XPathParseTool.parseXPath(xpath);
            } catch (XPathSyntaxException e) {
                InvalidStructureException toThrow = new InvalidStructureException(String.format(
                        "Invalid XPath Expression in QueryPrompt %s",
                        e.getMessage()), parser);
                toThrow.initCause(e);
                throw toThrow;
            }
        }
        return propertyValue;
    }
}
