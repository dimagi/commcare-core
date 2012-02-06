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
 * TODO: Soooooo un-thead-safe
 * 
 * @author ctsims
 *
 */
public class TreeElementCache {
	private Hashtable<Integer, TreeElement> cache;
	private int capacityDesired;
	private int currentCapacity;
	private Vector<Integer> cacheList;
	private Random random;
	private long totalMem;
	
	public TreeElementCache(int capacity) {
		cache = new Hashtable<Integer,TreeElement>();
		this.capacityDesired = capacity;
		this.currentCapacity = capacityDesired;
		random = new Random();
		cacheList = new Vector<Integer>();
		//Free Memory available from the start
		totalMem = Runtime.getRuntime().freeMemory();
	}

	public boolean hasEntry(int recordId) {
		return cache.containsKey(DataUtil.integer(recordId));
	}

	public TreeElement retrieve(int recordId) {
		return cache.get(DataUtil.integer(recordId));
	}

	public void register(int recordId, TreeElement item) {
		
		long freeMemory = Runtime.getRuntime().freeMemory();  
		double memory = freeMemory * 1.0 / totalMem;
		//Don't take up more than %90 of the available memory
		if(memory < .2) { 
			Runtime.getRuntime().gc();
			freeMemory = Runtime.getRuntime().freeMemory();  
			memory = freeMemory * 1.0 / totalMem;
		}
		if(memory < .2) {
			currentCapacity = Math.max(10, (int)(currentCapacity *.6));
			System.out.println("Cache Size Adjustment! New Size: " + currentCapacity);
			while(cache.size() > currentCapacity -1) {
				randomPop();
			}
		}
		
		
		if(cache.size() == currentCapacity) {
			if(currentCapacity < capacityDesired && memory > .3) {
				//If we've got plenty of memory, and we're at our current capacity,
				//throw some more memory at this problem and give back 50% of the reduced
				//capacity
				currentCapacity += (capacityDesired - currentCapacity) *.3;
				System.out.println("Cache Size Adjustment! New Size: " + currentCapacity);
			} else {
				for(int i = 0; i < 3; ++i ) {
					randomPop();
				}
			}
		}
		cache.put(new Integer(recordId), item);
		cacheList.addElement(new Integer(recordId));
	}
	
	private void randomPop() {
		int index = random.nextInt(cacheList.size());
		Integer elementToRemove = cacheList.elementAt(index);
		cacheList.removeElementAt(index);
		cache.remove(elementToRemove);
	}
	
}
