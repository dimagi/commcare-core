/**
 * 
 */
package org.commcare.cases.stock;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * Represents a set of stocks associated with an entity 
 * 
 * @author ctsims
 *
 */
public class Stock implements Persistable, IMetaData {

	//NOTE: Right now this is (lazily) implemented assuming that each stock
	//object tracks _all_ of the stocks for an entity, which will likely be a terrible way
	//to do things long-term. 
	
	
	public static final String STORAGE_KEY = "stock";
	public static final String INDEX_ENTITY_ID = "entity-id";

	String entityId;
	int recordId = -1;
	Hashtable<String, Hashtable<String, Integer>> stocks;
	
	public Stock() {
		
	}
	
	public Stock(String entityId) {
		this.entityId = entityId;
		this.stocks = new Hashtable<String, Hashtable<String, Integer>>();
	}
	
	/**
	 * Get the ID of the linked entity associated with this Stock record
	 * @return
	 */
	public String getEntiyId() {
		return entityId;
	}
	
	public int getProductValue(String stockId, String productId) {
		if(!stocks.containsKey(stockId) || !stocks.get(stockId).containsKey(productId)) {
			return 0;
		}
		return stocks.get(stockId).get(productId).intValue();
	}
	
	public String[] getStockList() {
		String[] productList = new String[stocks.size()];
		int i = 0;
		for(Enumeration e = stocks.keys(); e.hasMoreElements();) {
			productList[i] = (String)e.nextElement();
			++i;
		}
		return productList;

	}
	
	public String[] getProductList(String stockId) {
		Hashtable<String, Integer> products = stocks.get(stockId);
		String[] productList = new String[products.size()];
		int i = 0;
		for(Enumeration e = products.keys(); e.hasMoreElements();) {
			productList[i] = (String)e.nextElement();
			++i;
		}
		return productList;
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		recordId = ExtUtil.readInt(in);
		entityId = ExtUtil.readString(in);
		stocks = (Hashtable<String, Hashtable<String, Integer>>) ExtUtil.read(in, new ExtWrapMap(String.class, new ExtWrapMap(String.class, Integer.class)));
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, recordId);
		ExtUtil.writeString(out, entityId);
		ExtUtil.write(out, new ExtWrapMap(stocks, new ExtWrapMap(String.class, Integer.class)));
	}

	public void setID(int ID) {
		recordId = ID;
	}

	public int getID() {
		return recordId;
	}

	public void setProductValue(String stockId, String productId, int quantity) {
		if(!stocks.containsKey(stockId)) {
			stocks.put(stockId, new Hashtable<String, Integer>());
		}
		stocks.get(stockId).put(productId, new Integer(quantity));
	}

	public String[] getMetaDataFields() {
		return new String[] {INDEX_ENTITY_ID};
	}

	public Object getMetaData(String fieldName) {
		if(fieldName.equals(INDEX_ENTITY_ID)){
			return entityId;
		} else {
			throw new IllegalArgumentException("No metadata field " + fieldName  + " in the stock storage system");
		}
	}
}
