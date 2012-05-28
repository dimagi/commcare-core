/**
 * 
 */
package org.javarosa.core.util;

import org.javarosa.core.model.instance.TreeReferenceLevel;
import org.javarosa.core.util.externalizable.ExtUtil;

/**
 * J2ME suffers from major disparities in the effective use of memory. This
 * class encompasses some hacks that sadly have to happen pertaining to high
 * memory throughput actions.
 * 
 * This was implemented in a hurry, and urgently needs a major refactor to be less...
 * hacky, static, and stupid.
 * 
 * @author ctsims
 *
 */
public class MemoryUtils {
	static boolean oldterning;
	static boolean otrt; 
	public static void stopTerning() {
		oldterning = ExtUtil.interning;
		otrt = TreeReferenceLevel.treeRefLevelInterningEnabled;
		ExtUtil.interning = false;
		TreeReferenceLevel.treeRefLevelInterningEnabled = false;
	}
	
	public static void revertTerning() {
		ExtUtil.interning = oldterning;
		TreeReferenceLevel.treeRefLevelInterningEnabled = otrt;
	}
}
