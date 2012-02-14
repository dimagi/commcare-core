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
public class CacheTable<K> extends Hashtable<K, WeakReference> {
	private static Vector<WeakReference> caches = new Vector<WeakReference>();
	
	private static Thread cleaner = new Thread(new Runnable() {
		public void run() {
			while(true) {
			Vector<Integer> toRemove = new Vector<Integer>();
			for(int i = 0; i < caches.size() ; ++i) {
				CacheTable table = (CacheTable)caches.elementAt(i).get();
				if(table == null) {
					toRemove.addElement(DataUtil.integer(i));
				} else {
					for(Enumeration en = table.keys() ; en.hasMoreElements() ; ) {
						Object key = en.nextElement();
						if(((WeakReference)table.get(key)).get() == null) {
							table.remove(key);
						}
					}
				}
			}
			for(int id = toRemove.size() -1 ; id >= 0; --id) { 
				caches.removeElementAt(id);
			}
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
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
}
