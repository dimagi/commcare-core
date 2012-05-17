/**
 * 
 */
package org.commcare.util;

import org.javarosa.core.model.instance.TreeReferenceLevel;
import org.javarosa.core.util.CacheTable;
import org.javarosa.core.util.externalizable.ExtUtil;

/**
 * @author ctsims
 *
 */
public class CommCareStatic {
	//Holds all static reference stuff
	private static CacheTable<Integer> treeRefLevels;
	
	public static void init() {
		treeRefLevels = new CacheTable<Integer>();
		TreeReferenceLevel.attachCacheTable(treeRefLevels);
	}
	
	public static void cleanup() {
		treeRefLevels = null;
	}
}
