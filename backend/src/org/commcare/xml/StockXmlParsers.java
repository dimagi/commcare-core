/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.commcare.cases.stock.Stock;
import org.commcare.data.xml.TransactionParser;
import org.commcare.xml.util.InvalidStructureException;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Contains all of the logic for parsing transactions in xml that pertain to
 * ledgers (balance/transfer actions)
 * 
 * @author ctsims
 *
 */
public class StockXmlParsers extends TransactionParser<Stock[]> {
	private static final String TAG_QUANTITY = "quantity";

	private static final String TAG_VALUE = "value";

	private static final String ENTRY_ID = "id";

	private static final String TRANSFER = "transfer";

	private static final String TAG_BALANCE = "balance";

	public static final String STOCK_XML_NAMESPACE = "http://commtrack.org/stock_report";
	
	private static final String MODEL_ID = "entity-id";
	private static final String SUBMODEL_ID = "section-id";
	private static final String FINAL_NAME = "entry"; 
	

	IStorageUtilityIndexed<Stock> storage;
	
	/**
	 * Creates a Parser for case blocks in the XML stream provided. 
	 * 
	 * @param parser The parser for incoming XML.
	 */
	public StockXmlParsers(KXmlParser parser, IStorageUtilityIndexed<Stock> storage) {
		super(parser, null, null);
		this.storage = storage;
	}

	public Stock[] parse() throws InvalidStructureException, IOException, XmlPullParserException {
		this.checkNode(new String[] {TAG_BALANCE, TRANSFER});
		
		String name = parser.getName().toLowerCase();
	
		final Vector<Stock> toWrite = new Vector<Stock>();
		
		String dateModified = parser.getAttributeValue(null, "date");
		if(dateModified == null) { throw new InvalidStructureException("<" + name + "> block with no date_modified attribute.", this.parser); }
		Date modified = DateUtils.parseDateTime(dateModified);
		
		if(name.equals(TAG_BALANCE)) {
			String entityId = parser.getAttributeValue(null, MODEL_ID);
			if(entityId == null) { throw new InvalidStructureException("<balance> block with no " + MODEL_ID + " attribute.", this.parser); }
			
			final Stock ledger = retrieveOrCreate(entityId);

			//The stock ID being defined or not determines whether this is a per product stock update or an individual update
			String sectionId = parser.getAttributeValue(null, SUBMODEL_ID);
			
			if(sectionId == null) {
				//Complex case: we need to update multiple stocks on a per-product basis
				while(this.nextTagInBlock(TAG_BALANCE)) {
					
					//We need to capture some of the state (IE: Depth, etc) to parse recursively, 
					//so create a new anonymous parser.
					new ElementParser<Stock[]>(this.parser) {
						public Stock[] parse() throws InvalidStructureException, IOException,XmlPullParserException {
							String productId = parser.getAttributeValue(null, ENTRY_ID);
							
							//Walk through the value setters and pull out all of the quantities to be updated for this stock.
							while(this.nextTagInBlock(FINAL_NAME)) {
								this.checkNode(TAG_VALUE);
								
								String quantityString = parser.getAttributeValue(null, TAG_QUANTITY);
								String stockId = parser.getAttributeValue(null, SUBMODEL_ID);
								if(stockId == null || stockId == "") { throw new InvalidStructureException("<value> update requires a valid @" + SUBMODEL_ID + " attribute", this.parser); }
								int quantity = this.parseInt(quantityString);
								
								//This performs the actual modification. This entity will be written outside of the loop 
								ledger.setProductValue(stockId,productId, quantity);
							}
							return null;
						}
						
					}.parse();
				}
			} else {
				//Simple case - Updating one stock by its id.				
				while(this.nextTagInBlock(TAG_BALANCE)) {
					this.checkNode(FINAL_NAME);
					String id = parser.getAttributeValue(null, ENTRY_ID);
					String quantityString = parser.getAttributeValue(null, TAG_QUANTITY);
					if(id == null || id == "") { throw new InvalidStructureException("<" + FINAL_NAME + "> update requires a valid @id attribute", this.parser); }
					int quantity = this.parseInt(quantityString);
					ledger.setProductValue(sectionId, id, quantity);
				}				
			}
			
			//Either way, we've updated the stock and want to write it now
			toWrite.addElement(ledger);
		} else if(name.equals(TRANSFER)) {
			
			//First, figure out where we're reading/writing and load the ledgers 
			String source = parser.getAttributeValue(null, "src");
			String destination = parser.getAttributeValue(null, "dest");
			
			if(source == null && destination == null) { throw new InvalidStructureException("<transfer> block no source or destination id.", this.parser); }
			
			final Stock sourceLeger = source == null ? null : retrieveOrCreate(source);
			final Stock destinationLedger = destination == null ? null : retrieveOrCreate(destination);
			
			//The stock ID being defined or not determines whether this is a per product stock update or an individual update
			String sectionId = parser.getAttributeValue(null, SUBMODEL_ID);
			
			if(sectionId == null) {
				while(this.nextTagInBlock(TRANSFER)) {
					//We need to capture some of the state (IE: Depth, etc) to parse recursively, 
					//so create a new anonymous parser.
					new ElementParser<Stock[]>(this.parser) {
						public Stock[] parse() throws InvalidStructureException, IOException,XmlPullParserException {
							String productId = parser.getAttributeValue(null, ENTRY_ID);
							
							//Walk through and find what sections to update for this entry 
							while(this.nextTagInBlock(FINAL_NAME)) {
								this.checkNode(TAG_VALUE);
								
								String quantityString = parser.getAttributeValue(null, TAG_QUANTITY);
								String sectionId = parser.getAttributeValue(null, SUBMODEL_ID);
								if(sectionId == null || sectionId == "") { throw new InvalidStructureException("<value> update requires a valid @" + SUBMODEL_ID + " attribute", this.parser); }
								int quantity = this.parseInt(quantityString);

								if(sourceLeger != null) {
									sourceLeger.setProductValue(sectionId, productId, sourceLeger.getProductValue(sectionId, productId) - quantity);
								}
								if(destinationLedger != null) {
									destinationLedger.setProductValue(sectionId, productId, destinationLedger.getProductValue(sectionId, productId) + quantity);
								}
							}
							return null;
						}
						
					}.parse();
				}
			} else {
				//Easy case, we've got a single section and we're going to transfer between the ledgers
				while(this.nextTagInBlock(TRANSFER)) {
					this.checkNode(FINAL_NAME);
					String entryId = parser.getAttributeValue(null, ENTRY_ID);
					String quantityString = parser.getAttributeValue(null, TAG_QUANTITY);
					if(entryId == null || entryId == "") { throw new InvalidStructureException("<" + FINAL_NAME + "> update requires a valid @" + ENTRY_ID + " attribute", this.parser); }
					int quantity = this.parseInt(quantityString);
					
					if(sourceLeger != null) {
						sourceLeger.setProductValue(sectionId, entryId, sourceLeger.getProductValue(sectionId, entryId) - quantity);
					}
					if(destinationLedger != null) {
						destinationLedger.setProductValue(sectionId, entryId, destinationLedger.getProductValue(sectionId, entryId) + quantity);
					}
				}
			}
			
			//Either way, we want to now write both stocks.
			if(sourceLeger != null) {
				toWrite.addElement(sourceLeger);
			}
			if(destinationLedger != null) {
				toWrite.addElement(destinationLedger);
			}
		}
		
		Stock[] tw = new Stock[toWrite.size()];
		int i =0;
		for(Stock s : toWrite) {
			tw[i] = s;
			i++;
		}
		//this should really be decided on _not_ in the parser...
		commit(tw);
		
		return tw;
	}		

	public void commit(Stock[] parsed) throws IOException {
		try {
			for(Stock s : parsed) {
				storage().write(s);
			}
		} catch (StorageFullException e) {
			e.printStackTrace();
			throw new IOException("Storage full while writing case!");
		}
	}

	public Stock retrieveOrCreate(String entityId) {
		try{
			return (Stock)storage().getRecordForValue(Stock.INDEX_ENTITY_ID, entityId);
		} catch(NoSuchElementException nsee) {
			return new Stock(entityId);
		}
	}
	
	public IStorageUtilityIndexed<Stock> storage() {
		return storage;
	}

}
