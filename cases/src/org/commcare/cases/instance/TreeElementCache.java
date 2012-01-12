/**
 * 
 */
package org.commcare.cases.instance;

import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.util.DataUtil;

/**
 * @author ctsims
 *
 */
public class TreeElementCache {
	private Hashtable<Integer, TreeElement> cache;
	private int capacity;
	private Vector<Integer> cacheList;
	private Random random;
	
	public TreeElementCache(int capacity) {
		cache = new Hashtable<Integer,TreeElement>();
		this.capacity = capacity;
		random = new Random();
		cacheList = new Vector<Integer>();
	}

	public boolean hasEntry(int recordId) {
		return cache.containsKey(DataUtil.integer(recordId));
	}

	public TreeElement retrieve(int recordId) {
		return cache.get(DataUtil.integer(recordId));
	}

	public void register(int recordId, TreeElement item) {
		if(cache.size() == capacity) {
			for(int i = 0; i < 3; ++i ) {
				int index = random.nextInt(cacheList.size());
				Integer elementToRemove = cacheList.elementAt(index);
				cacheList.removeElementAt(index);
				cache.remove(elementToRemove);
			}
		}
		cache.put(new Integer(recordId), item);
		cacheList.addElement(new Integer(recordId));
	}
	
}
