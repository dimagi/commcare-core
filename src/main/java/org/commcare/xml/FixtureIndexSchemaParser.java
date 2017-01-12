package org.commcare.xml;

import org.commcare.cases.instance.FixtureIndexSchema;
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
 * Parses fixture index schemas into an object representation:
 *
 * <table-indices id="some-fixture-name">
 *     <index>some-index</index>
 *     <index>name,some-index</index>
 * </table-indices>
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FixtureIndexSchemaParser extends TransactionParser<FixtureIndexSchema> {
    public final static String INDICE_SCHEMA = "table-indices";

    private final Map<String, FixtureIndexSchema> fixtureSchemas;

    public FixtureIndexSchemaParser(KXmlParser parser, Map<String, FixtureIndexSchema> fixtureSchemas) {
        super(parser);
        this.fixtureSchemas = fixtureSchemas;
    }

    @Override
    protected void commit(FixtureIndexSchema parsedSchema) throws IOException, InvalidStructureException {
        fixtureSchemas.put(parsedSchema.fixtureName, parsedSchema);
    }

    @Override
    public FixtureIndexSchema parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        checkNode(INDICE_SCHEMA);

        String fixtureId = parser.getAttributeValue(null, "id");
        if (fixtureId == null) {
            throw new InvalidStructureException(INDICE_SCHEMA + " is lacking an 'id' attribute", parser);
        }

        TreeElement root = new TreeElementParser(parser, 0, fixtureId).parse();
        FixtureIndexSchema schema = new FixtureIndexSchema(root);
        commit(schema);
        return schema;
    }
}
