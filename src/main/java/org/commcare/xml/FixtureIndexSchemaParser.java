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
import java.util.Set;

/**
 * Parses fixture index schemas into an object representation:
 *
 * <schema id="some-fixture-name">
 *   <indices>
 *     <index>some-index</index>
 *     <index>name,some-index</index>
 *   </indices>
 * </schema>
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FixtureIndexSchemaParser extends TransactionParser<FixtureIndexSchema> {
    public final static String INDICE_SCHEMA = "schema";

    private final Map<String, FixtureIndexSchema> fixtureSchemas;
    private final Set<String> processedFixtures;

    public FixtureIndexSchemaParser(KXmlParser parser,
                                    Map<String, FixtureIndexSchema> fixtureSchemas,
                                    Set<String> processedFixtures) {
        super(parser);
        this.fixtureSchemas = fixtureSchemas;
        this.processedFixtures = processedFixtures;
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

        if (processedFixtures.contains(fixtureId)) {
            throw new InvalidStructureException(INDICE_SCHEMA + " for '" + fixtureId
                    + "' appeared after fixture in the user restore", parser);
        }

        TreeElement root = new TreeElementParser(parser, 0, fixtureId).parse();
        FixtureIndexSchema schema = new FixtureIndexSchema(root.getChild("indices", 0), fixtureId);
        commit(schema);
        return schema;
    }
}
