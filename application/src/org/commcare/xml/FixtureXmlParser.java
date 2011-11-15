/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.commcare.data.xml.TransactionParser;
import org.commcare.xml.util.InvalidStructureException;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.cases.model.Case;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
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
public class FixtureXmlParser extends TransactionParser<FormInstance> {

	IStorageUtilityIndexed storage;
	
	/**
	 * Creates a Parser for case blocks in the XML stream provided. 
	 * 
	 * @param parser The parser for incoming XML.
	 * @param tallies an int[3] array to place information about the parser's actions.
	 * @param acceptCreateOverwrites Whether an Exception should be thrown if the transaction
	 * contains create actions for cases which already exist.
	 */
	public FixtureXmlParser(KXmlParser parser) {
		super(parser, "fixture", null);
	}

	public FormInstance parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
		this.checkNode("fixture");
		
		String fixtureId = parser.getAttributeValue(null, "id");
		if(fixtureId == null) {
			throw new InvalidStructureException("fixture is lacking id attribute", parser);
		}
		
		//Get to the data root
		parser.nextTag();
		
		TreeElement root = new TreeElementParser(parser, 0, fixtureId).parse();
		FormInstance instance = new FormInstance(root, fixtureId);
		try {
			storage().write(instance);
		} catch (StorageFullException e) {
			e.printStackTrace();
			throw new IOException("Storage full while writing fixture!");
		}

		return instance;
	}

	public void commit(FormInstance parsed) throws IOException {
		try {
			storage().write(parsed);
		} catch (StorageFullException e) {
			e.printStackTrace();
			throw new IOException("Storage full while writing case!");
		}
	}

	public IStorageUtilityIndexed storage() {
		if(storage == null) {
			storage = (IStorageUtilityIndexed)StorageManager.getStorage("fixture");
		} 
		return storage;
	}
}
