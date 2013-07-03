/**
 * 
 */
package org.commcare.util;

import org.javarosa.core.model.instance.TreeReferenceLevel;
import org.javarosa.core.util.CacheTable;
import org.javarosa.xpath.expr.XPathStep;

/**
 * @author ctsims
 *
 */
public class CommCareStatic {
	//Holds all static reference stuff
	private static CacheTable<Integer> treeRefLevels;
	private static CacheTable<Integer> xpathSteps;
	
	public static void init() {
		treeRefLevels = new CacheTable<Integer>();
		xpathSteps = new CacheTable<Integer>();
		TreeReferenceLevel.attachCacheTable(treeRefLevels);
		XPathStep.attachCacheTable(xpathSteps);
	}
	
	public static void cleanup() {
		treeRefLevels = null;
	}
}
