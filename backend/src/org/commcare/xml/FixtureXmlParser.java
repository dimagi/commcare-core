/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Vector;

import org.commcare.data.xml.TransactionParser;
import org.commcare.xml.util.InvalidStructureException;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.externalizable.ExtUtil;
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
	boolean overwrite = true;
	
	public FixtureXmlParser(KXmlParser parser) {
		this(parser, true);
	}
	
	public FixtureXmlParser(KXmlParser parser, boolean overwrite) {
		super(parser, "fixture", null);
		this.overwrite = overwrite;
	}

	public FormInstance parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
		this.checkNode("fixture");
		
		String fixtureId = parser.getAttributeValue(null, "id");
		if(fixtureId == null) {
			throw new InvalidStructureException("fixture is lacking id attribute", parser);
		}
		
		String userId = parser.getAttributeValue(null, "user_id");
		
		//Get to the data root
		parser.nextTag();
		
		//TODO: We need to overwrite any matching records here.
		TreeElement root = new TreeElementParser(parser, 0, fixtureId).parse();
		FormInstance instance = new FormInstance(root, fixtureId);
		
		//This is a terrible hack and clayton should feeel terrible about it
		if(userId != null) { 
			instance.schema = userId;
		}
		
		int recordId = -1;
		Vector<Integer> matchingFixtures = storage().getIDsForValue(FormInstance.META_ID, fixtureId);
		if(matchingFixtures.size() > 0) {
			//find all fixtures with the same user
			Vector<Integer> matchingUsers = storage().getIDsForValue(FormInstance.META_XMLNS, ExtUtil.emptyIfNull(userId));
			for(Integer i : matchingFixtures) {
				if(matchingUsers.indexOf(i) != -1) {
					recordId = i.intValue();
				}
			}
					
		}
		
		if(recordId != -1) {
			if(!overwrite) {
				//parse it out, but don't write anything to memory if one already exists
				return instance;
			}
			instance.setID(recordId);
		}
		
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
