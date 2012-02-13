/**
 * 
 */
package org.commcare.cases.instance;

import java.lang.ref.WeakReference;
import java.util.Hashtable;

import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.util.DataUtil;

/**
 * TODO: Soooooo un-thead-safe
 * 
 * @author ctsims
 *
 */
public class TreeElementCache {
	private Hashtable<Integer, WeakReference> cache;
	
	public TreeElementCache() {
		cache = new Hashtable<Integer,WeakReference>();
	}

	public TreeElement retrieve(int recordId) {
		if(!cache.containsKey(DataUtil.integer(recordId))) { return null; }
		TreeElement retVal = (TreeElement)cache.get(DataUtil.integer(recordId)).get();
		if(retVal == null) { cache.remove(DataUtil.integer(recordId)); }
		return retVal;
	}

	public void register(int recordId, TreeElement item) {
		cache.put(new Integer(recordId), new WeakReference(item));
	}
}
