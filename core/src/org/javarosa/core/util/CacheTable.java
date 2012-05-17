/**
 * 
 */
package org.javarosa.core.util;

import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.instance.TreeReferenceLevel;

/**
 * @author ctsims
 *
 */
public class CacheTable<K> extends Hashtable<K, WeakReference> {
	private static Vector<WeakReference> caches = new Vector<WeakReference>();
	
	private static Thread cleaner = new Thread(new Runnable() {
		public void run() {
			Vector<Integer> toRemove = new Vector<Integer>();			
			while(true) {
				try {
					toRemove.removeAllElements();
					for (int i = 0; i < caches.size(); ++i) {
						CacheTable table = (CacheTable) caches.elementAt(i).get();
						if (table == null) {
							toRemove.addElement(DataUtil.integer(i));
						} else {
							int start = table.size();
							for (Enumeration en = table.keys(); en.hasMoreElements();) {
								Object key = en.nextElement();
								if (((WeakReference) table.get(key)).get() == null) {
									table.remove(key);
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
		registerCache(this);
	}
	
	public <V> V intern(V v) {
		Integer hashCode = DataUtil.integer(v.hashCode());
		if(containsKey(hashCode)) {
			V nv = (V)get(hashCode).get();
			if(nv == null) { this.put((K)hashCode, new WeakReference(v)); return v;};
			if(nv.equals(v)) { return v;}
			return v;
		} 
		put((K)hashCode, new WeakReference(v));
		return v;

	}
}
