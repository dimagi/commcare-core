package org.commcare.xml;

import org.commcare.suite.model.Endpoint;
import org.commcare.suite.model.StackOperation;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Vector;

public class EndpointParser extends ElementParser<Endpoint> {

    static final String NAME_ENDPOINT = "endpoint";
    private static final String NAME_ARGUMENT = "argument";
    private static final String NAME_STACK = "stack";

    public EndpointParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public Endpoint parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        String endpointId = parser.getAttributeValue(null, "id");
        if (endpointId == null || endpointId.isEmpty()) {
            throw new InvalidStructureException("endpoint must define a non empty id", parser);
        }

        Vector<StackOperation> stackOperations = new Vector<>();
        Vector<String> arguments = new Vector<>();

        while (nextTagInBlock(NAME_ENDPOINT)) {
            String tagName = parser.getName().toLowerCase();
            if (tagName.contentEquals(NAME_ARGUMENT)) {
                String argumentID = parser.getAttributeValue(null, "id");
                if (argumentID == null || argumentID.isEmpty()) {
                    throw new InvalidStructureException("argument must define a non empty id", parser);
                }
                arguments.add(argumentID);
            } else if (tagName.contentEquals(NAME_STACK)) {
                StackOpParser sop = new StackOpParser(parser);
                while (this.nextTagInBlock(NAME_STACK)) {
                    stackOperations.add(sop.parse());
                }
            }
        }

        return new Endpoint(endpointId, arguments, stackOperations);
    }
}
