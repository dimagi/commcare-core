package org.commcare.xml;

import org.commcare.cases.instance.FlatFixtureSchema;
import org.commcare.data.xml.TransactionParser;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Map;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FixtureSchemaParser extends TransactionParser<FlatFixtureSchema> {

    private final Map<String, FlatFixtureSchema> fixtureSchemas;

    public FixtureSchemaParser(KXmlParser parser, Map<String, FlatFixtureSchema> fixtureSchemas) {
        super(parser);
        this.fixtureSchemas = fixtureSchemas;
    }

    @Override
    protected void commit(FlatFixtureSchema parsedSchema) throws IOException, InvalidStructureException {
        fixtureSchemas.put(parsedSchema.fixtureName, parsedSchema);
    }

    @Override
    public FlatFixtureSchema parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        checkNode("schema");

        String fixtureId = parser.getAttributeValue(null, "id");
        if (fixtureId == null) {
            throw new InvalidStructureException("fixture is lacking id attribute", parser);
        }

        // only commit fixtures with bodies to storage
        TreeElement root = new TreeElementParser(parser, 0, fixtureId).parse();
        FlatFixtureSchema schema = new FlatFixtureSchema(root);
        commit(schema);
        return schema;
    }
}
