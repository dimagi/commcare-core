/**
 * 
 */
package org.javarosa.core.util;

import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author ctsims
 *
 */
public class CacheTable<K> {
	int largestSize = 0;
	
	private Hashtable<Integer, WeakReference> currentTable;
	
	private static Vector<WeakReference> caches = new Vector<WeakReference>();
	
	private static Thread cleaner = new Thread(new Runnable() {
		public void run() {
			Vector<Integer> toRemove = new Vector<Integer>();			
			while(true) {
				try {
					toRemove.removeAllElements();
					for (int i = 0; i < caches.size(); ++i) {
						CacheTable cache = (CacheTable) caches.elementAt(i).get();
						if (cache == null) {
							toRemove.addElement(DataUtil.integer(i));
						} else {
							Hashtable<Integer, WeakReference> table = cache.currentTable;
							int start = table.size();
							if(start > cache.largestSize) { cache.largestSize = start; }
							for (Enumeration en = table.keys(); en.hasMoreElements();) {
								Object key = en.nextElement();
								if (((WeakReference) table.get(key)).get() == null) {
									table.remove(key);
								}
							}
							
							synchronized(cache) {
								//See if our current size is 12.5% the size of the largest size we've been
								//and compact (clone to a new table) if so
								//TODO: 50 is a super arbitrary upper bound
								if(cache.largestSize > 50 && cache.largestSize > (cache.currentTable.size() >> 2) ) {
									Hashtable newTable = new Hashtable(cache.currentTable.size());
									for (Enumeration en = table.keys(); en.hasMoreElements();) {
										Object key = en.nextElement();
										newTable.put(key, cache.currentTable.get(key));
									}
									cache.currentTable = newTable;
									cache.largestSize = cache.currentTable.size();
								}
							}
							
						}
					}
					for (int id = toRemove.size() - 1; id >= 0; --id) {
						caches.removeElementAt(toRemove.elementAt(id));
					}
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
	});
	
	private static void registerCache(CacheTable table) {
		caches.addElement(new WeakReference(table));
		synchronized(cleaner) {
			if(!cleaner.isAlive()) {
				cleaner.start();
			}
		}
	}
	
	public CacheTable() {
		super();
		currentTable = new Hashtable<Integer, WeakReference>();
		registerCache(this);
	}
	
	public K intern(K k) {
		synchronized(this) {
			int hash = k.hashCode();
			K nk = retrieve(hash);
			if(nk == null) {
				register(hash, k);
				return k;
			}
			if(k.equals(nk)) {
				return nk;
			}
			return k;
		}
	}
	

	public K retrieve(int key) {
		synchronized(this) {
			if(!currentTable.containsKey(DataUtil.integer(key))) { return null; }
			K retVal = (K)currentTable.get(DataUtil.integer(key)).get();
			if(retVal == null) { currentTable.remove(DataUtil.integer(key)); }
			return retVal;
		}
	}

	public void register(int key, K item) {
		synchronized(this) {
			currentTable.put(DataUtil.integer(key), new WeakReference(item));
		}
	}
}
