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
 * The CaseXML Parser is responsible for processing and performing
 * case transactions from an incoming XML stream. It will perform
 * all of the actions specified by the transaction (Create/modify/close)
 * against the application's current storage. 
 * 
 * @author ctsims
 *
 */
public class StockXmlParsers extends TransactionParser<Stock[]> {
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
		this.checkNode(new String[] {"balance", "transfer"});
		
		String name = parser.getName().toLowerCase();
	
		final Vector<Stock> toWrite = new Vector<Stock>();
		
		String dateModified = parser.getAttributeValue(null, "date");
		if(dateModified == null) { throw new InvalidStructureException("<" + name + "> block with no date_modified attribute.", this.parser); }
		Date modified = DateUtils.parseDateTime(dateModified);
		
		if(name.equals("balance")) {
			String entityId = parser.getAttributeValue(null, MODEL_ID);
			if(entityId == null) { throw new InvalidStructureException("<balance> block with no " + MODEL_ID + " attribute.", this.parser); }
			
			final Stock s = retrieveOrCreate(entityId);

			//The stock ID being defined or not determines whether this is a per product stock update or an individual update
			String stockId = parser.getAttributeValue(null, SUBMODEL_ID);
			
			if(stockId == null) {
				//Complex case: we need to update multiple stocks on a per-product basis
				while(this.nextTagInBlock("balance")) {
					new ElementParser<Stock[]>(this.parser) {
						public Stock[] parse() throws InvalidStructureException, IOException,XmlPullParserException {
							String productId = parser.getAttributeValue(null, "id");
							while(this.nextTagInBlock(FINAL_NAME)) {
								this.checkNode("value");
								
								String quantityString = parser.getAttributeValue(null, "quantity");
								String stockId = parser.getAttributeValue(null, SUBMODEL_ID);
								if(stockId == null || stockId == "") { throw new InvalidStructureException("<value> update requires a valid @" + SUBMODEL_ID + " attribute", this.parser); }
								int quantity = this.parseInt(quantityString);
								s.setProductValue(stockId,productId, quantity);

								
							}
							return null;
						}
						
					}.parse();
				}
			} else {
			
				//Simple case - Updating one stock by its id.				
				while(this.nextTagInBlock("balance")) {
					this.checkNode(FINAL_NAME);
					String id = parser.getAttributeValue(null, "id");
					String quantityString = parser.getAttributeValue(null, "quantity");
					if(id == null || id == "") { throw new InvalidStructureException("<" + FINAL_NAME + "> update requires a valid @id attribute", this.parser); }
					int quantity = this.parseInt(quantityString);
					s.setProductValue(stockId, id, quantity);
				}				
			}
			
			//Either way, we've updated the stock and want to write it now
			toWrite.addElement(s);
		} else if(name.equals("transfer")) {
			String source = parser.getAttributeValue(null, "src");
			String destination = parser.getAttributeValue(null, "dest");
			
			if(source == null && destination == null) { throw new InvalidStructureException("<transfer> block no source or destination id.", this.parser); }
			
			final Stock sourceStock = source == null ? null : retrieveOrCreate(source);
			
			final Stock destinationStock = destination == null ? null : retrieveOrCreate(destination);
			
			//The stock ID being defined or not determines whether this is a per product stock update or an individual update
			String stockId = parser.getAttributeValue(null, SUBMODEL_ID);
			
			if(stockId == null) {
				while(this.nextTagInBlock("transfer")) {
					new ElementParser<Stock[]>(this.parser) {
						public Stock[] parse() throws InvalidStructureException, IOException,XmlPullParserException {
							String productId = parser.getAttributeValue(null, "id");
							while(this.nextTagInBlock(FINAL_NAME)) {
								this.checkNode("value");
								
								String quantityString = parser.getAttributeValue(null, "quantity");
								String stockId = parser.getAttributeValue(null, SUBMODEL_ID);
								if(stockId == null || stockId == "") { throw new InvalidStructureException("<value> update requires a valid @" + SUBMODEL_ID + " attribute", this.parser); }
								int quantity = this.parseInt(quantityString);

								if(sourceStock != null) {
									sourceStock.setProductValue(stockId, productId, sourceStock.getProductValue(stockId, productId) - quantity);
								}
								if(destinationStock != null) {
									destinationStock.setProductValue(stockId, productId, destinationStock.getProductValue(stockId, productId) + quantity);
								}
							}
							return null;
						}
						
					}.parse();
				}
			} else {
				while(this.nextTagInBlock("transfer")) {
					this.checkNode(FINAL_NAME);
					String productId = parser.getAttributeValue(null, "id");
					String quantityString = parser.getAttributeValue(null, "quantity");
					if(productId == null || productId == "") { throw new InvalidStructureException("<" + FINAL_NAME + "> update requires a valid @id attribute", this.parser); }
					int quantity = this.parseInt(quantityString);
					
					if(sourceStock != null) {
						sourceStock.setProductValue(stockId, productId, sourceStock.getProductValue(stockId, productId) - quantity);
					}
					if(destinationStock != null) {
						destinationStock.setProductValue(stockId, productId, destinationStock.getProductValue(stockId, productId) + quantity);
					}
				}
			}
			
			//Either way, we want to now write both stocks.
			if(sourceStock != null) {
				toWrite.addElement(sourceStock);
			}
			if(destinationStock != null) {
				toWrite.addElement(destinationStock);
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
