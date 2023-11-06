package org.commcare.xml;

import org.commcare.suite.model.Endpoint;
import org.commcare.suite.model.EndpointArgument;
import org.commcare.suite.model.StackOperation;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Vector;

import static org.commcare.xml.StackOpParser.NAME_STACK;
import static org.javarosa.core.model.instance.ExternalDataInstance.JR_SELECTED_ENTITIES_REFERENCE;

import com.google.common.collect.ImmutableList;

public class EndpointParser extends ElementParser<Endpoint> {

    static final String NAME_ENDPOINT = "endpoint";
    private static final String NAME_ARGUMENT = "argument";
    private static final String ATTR_ARGUMENT_ID = "id";
    private static final String ATTR_ARGUMENT_RESPECT_RELEVANCY = "respect-relevancy";
    private static final String ATTR_ARGUMENT_INSTANCE_ID = "instance-id";
    private static final String ATTR_ARGUMENT_INSTANCE_SRC = "instance-src";


    public EndpointParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public Endpoint parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        String endpointId = parser.getAttributeValue(null, "id");
        if (endpointId == null || endpointId.isEmpty()) {
            throw new InvalidStructureException("endpoint must define a non empty id", parser);
        }

        String respectRelevancyStr = parser.getAttributeValue(null, ATTR_ARGUMENT_RESPECT_RELEVANCY);

        // we are defaulting to true by checking against "false"
        boolean respectReslevancy = !"false".equals(respectRelevancyStr);

        Vector<StackOperation> stackOperations = new Vector<>();
        Vector<EndpointArgument> arguments = new Vector<>();

        while (nextTagInBlock(NAME_ENDPOINT)) {
            String tagName = parser.getName().toLowerCase();
            if (tagName.contentEquals(NAME_ARGUMENT)) {
                String argumentID = parser.getAttributeValue(null, ATTR_ARGUMENT_ID);
                if (argumentID == null || argumentID.isEmpty()) {
                    throw new InvalidStructureException("argument must define a non empty id", parser);
                }
                String argInstanceId = parser.getAttributeValue(null, ATTR_ARGUMENT_INSTANCE_ID);
                String argInstanceSrc = parser.getAttributeValue(null, ATTR_ARGUMENT_INSTANCE_SRC);

                if (argInstanceId != null && argInstanceSrc == null) {
                    throw new InvalidStructureException(
                            "Endpoint argument containing a non-null instance-id must define an instance-src", parser);
                }

                ImmutableList<String> validInstanceSrc = ImmutableList.of(JR_SELECTED_ENTITIES_REFERENCE);
                if (argInstanceSrc != null && !validInstanceSrc.contains(argInstanceSrc)) {
                    throw new InvalidStructureException(
                            "instance-src for an endpoint argument must be one of " + validInstanceSrc, parser);
                }

                arguments.add(new EndpointArgument(argumentID, argInstanceId, argInstanceSrc));
            } else if (tagName.contentEquals(NAME_STACK)) {
                StackOpParser sop = new StackOpParser(parser);
                while (this.nextTagInBlock(NAME_STACK)) {
                    stackOperations.add(sop.parse());
                }
            }
        }

        return new Endpoint(endpointId, arguments, stackOperations, respectReslevancy);
    }
}
