/**
 *
 */
package org.javarosa.engine.xml;

import java.io.IOException;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * The CaseXML Parser is responsible for processing and performing
 * case transactions from an incoming XML stream. It will perform
 * all of the actions specified by the transaction (Create/modify/close)
 * against the application's current storage.
 *
 * @author ctsims
 *
 */
public class FormInstanceParser extends ElementParser<FormInstance> {

    public FormInstanceParser(KXmlParser parser) {
        super(parser);
    }

    public FormInstance parse() throws InvalidStructureException, IOException, XmlPullParserException {
        this.checkNode("instance");

        String instanceId = parser.getAttributeValue(null, "src");
        if(instanceId == null) {
            throw new InvalidStructureException("Instance lacking src", parser);
        }

        //Get to the data root
        parser.nextTag();

        //TODO: We need to overwrite any matching records here.
        TreeElement root = new TreeElementParser(parser, 0, null).parse();
        FormInstance instance = new FormInstance(root, instanceId);

        return instance;
    }
}
