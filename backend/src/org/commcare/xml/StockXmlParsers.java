/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.commcare.cases.stock.Stock;
import org.commcare.data.xml.TransactionParser;
import org.commcare.xml.util.InvalidStructureException;
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

	IStorageUtilityIndexed<Stock> storage;
	
	/**
	 * Creates a Parser for case blocks in the XML stream provided. 
	 * 
	 * @param parser The parser for incoming XML.
	 */
	public StockXmlParsers(KXmlParser parser, IStorageUtilityIndexed<Stock> storage) {
		super(parser, "case", null);
		this.storage = storage;
	}

	public Stock[] parse() throws InvalidStructureException, IOException, XmlPullParserException {
		this.checkNode(new String[] {"balance", "transfer"});
		
		String name = parser.getName().toLowerCase();
	
		Vector<Stock> toWrite = new Vector<Stock>();
		
		String dateModified = parser.getAttributeValue(null, "date");
		if(dateModified == null) { throw new InvalidStructureException("<" + name + "> block with no date_modified attribute.", this.parser); }
		Date modified = DateUtils.parseDateTime(dateModified);
		
		if(name.equals("balance")) {
			String entityId = parser.getAttributeValue(null, "entity-id");
			if(entityId == null) { throw new InvalidStructureException("<balance> block with no entity-id attribute.", this.parser); }
			
			Stock s = retrieve(entityId);
			if(s == null) {
				s = new Stock(entityId, new Hashtable<String, Integer>());
			}
			
			while(this.nextTagInBlock("balance")) {
				this.checkNode("product");
				String id = parser.getAttributeValue(null, "id");
				String quantityString = parser.getAttributeValue(null, "quantity");
				if(id == null || id == "") { throw new InvalidStructureException("<product> update requires a valid @id attribute", this.parser); }
				int quantity = this.parseInt(quantityString);
				s.setProductValue(id, quantity);
			}
			
			toWrite.addElement(s);
		} else if(name.equals("transfer")) {
			String source = parser.getAttributeValue(null, "source");
			if(source == null) { throw new InvalidStructureException("<transfer> block with no source id.", this.parser); }
			
			String destination = parser.getAttributeValue(null, "destination");
			if(destination == null) { throw new InvalidStructureException("<transfer> block with no destination id.", this.parser); }
			
			Stock sourceStock = retrieve(source);
			if(sourceStock == null) {
				sourceStock = new Stock(source, new Hashtable<String, Integer>());
			}
			Stock destinationStock = retrieve(destination);
			if(destinationStock == null) {
				destinationStock = new Stock(destination, new Hashtable<String, Integer>());
			}
			
			while(this.nextTagInBlock("transfer")) {
				this.checkNode("product");
				String productId = parser.getAttributeValue(null, "id");
				String quantityString = parser.getAttributeValue(null, "quantity");
				if(productId == null || productId == "") { throw new InvalidStructureException("<product> update requires a valid @id attribute", this.parser); }
				int quantity = this.parseInt(quantityString);
				
				sourceStock.setProductValue(productId, sourceStock.getProductValue(productId) - quantity);
				destinationStock.setProductValue(productId, destinationStock.getProductValue(productId) + quantity);
			}
			toWrite.addElement(sourceStock);
			toWrite.addElement(destinationStock);
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

	public Stock retrieve(String entityId) {
		try{
			return (Stock)storage().getRecordForValue(Stock.INDEX_ENTITY_ID, entityId);
		} catch(NoSuchElementException nsee) {
			return null;
		}
	}
	
	public IStorageUtilityIndexed<Stock> storage() {
		return storage;
	}

}
