/**
 * 
 */
package org.commcare.util;

import org.javarosa.core.model.instance.TreeReferenceLevel;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.CacheTable;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.xform.parse.XFormParserFactory;
import org.javarosa.xform.util.XFormUtils;
import org.javarosa.xpath.expr.XPathStep;

/**
 * @author ctsims
 *
 */
public class CommCareStatic {
	//Holds all static reference stuff
	private static CacheTable<TreeReferenceLevel> treeRefLevels;
	private static CacheTable<XPathStep> xpathSteps;
	public static CacheTable<String> appStringCache;
	
	public static void init() {
		treeRefLevels = new CacheTable<TreeReferenceLevel>();
		xpathSteps = new CacheTable<XPathStep>();
		appStringCache= new CacheTable<String>();
		TreeReferenceLevel.attachCacheTable(treeRefLevels);
		XPathStep.attachCacheTable(xpathSteps);
		ExtUtil.attachCacheTable(appStringCache);
		
		XFormUtils.setXFormParserFactory(new XFormParserFactory(appStringCache));
	}
	
	public static void cleanup() {
		//TODO: This doesn't do anything, we need to, like, tell them to clean up their internals instead.
		treeRefLevels = null;
		xpathSteps = null;
		appStringCache = null;
	}
}
