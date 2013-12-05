/**
 * 
 */
package org.commcare.cases.stock;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class Stock implements Persistable {

	public static final String STORAGE_KEY = "stock";
	public static final String INDEX_ENTITY_ID = "entity-id";

	String entityId;
	int recordId = -1;
	Hashtable<String, Integer> products;
	
	public Stock() {
		
	}
	
	public Stock(String entityId, Hashtable<String, Integer> products) {
		this.entityId = entityId;
		this.products = products;
	}
	
	/**
	 * Get the ID of the linked entity associated with this Stock record
	 * @return
	 */
	public String getEntiyId() {
		return entityId;
	}
	
	public int getProductValue(String productId) {
		if(!products.containsKey(productId)) {
			return 0;
		}
		return products.get(productId).intValue();
	}
	
	public String[] getProductList() {
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
		products = (Hashtable<String, Integer>) ExtUtil.read(in, new ExtWrapMap(String.class, Integer.class));
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, recordId);
		ExtUtil.writeString(out, entityId);
		ExtUtil.write(out, new ExtWrapMap(products));
	}

	public void setID(int ID) {
		recordId = ID;
	}

	public int getID() {
		return recordId;
	}

	public void setProductValue(String id, int quantity) {
		products.put(id, new Integer(quantity));
	}
	
	
}
