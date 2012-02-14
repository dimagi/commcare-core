/**
 * 
 */
package org.commcare.cases.instance;

import java.lang.ref.WeakReference;

import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.util.CacheTable;
import org.javarosa.core.util.DataUtil;

/**
 * TODO: Soooooo un-thead-safe
 * 
 * @author ctsims
 *
 */
public class TreeElementCache {
	private CacheTable<Integer> cache;
	
	public TreeElementCache() {
		cache = new CacheTable<Integer>();
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
