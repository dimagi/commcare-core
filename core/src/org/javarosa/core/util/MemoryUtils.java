/**
 * 
 */
package org.javarosa.core.util;

import org.javarosa.core.model.instance.TreeReferenceLevel;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.xpath.expr.XPathStep;

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
	static boolean oldxpath;
	public static void stopTerning() {
		oldterning = ExtUtil.interning;
		otrt = TreeReferenceLevel.treeRefLevelInterningEnabled;
		oldxpath = XPathStep.XPathStepInterningEnabled;
		ExtUtil.interning = false;
		TreeReferenceLevel.treeRefLevelInterningEnabled = false;
		XPathStep.XPathStepInterningEnabled = false;
	}
	
	public static void revertTerning() {
		ExtUtil.interning = oldterning;
		TreeReferenceLevel.treeRefLevelInterningEnabled = otrt;
		XPathStep.XPathStepInterningEnabled = oldxpath;
	}
	
	//#if javarosa.memory.print
	//# private static boolean MEMORY_PRINT_ENABLED = true;
	//#else
	private static boolean MEMORY_PRINT_ENABLED = false;
	//#endif
	
	public static void printMemoryTest() {
		printMemoryTest(null);
	}
	
	public static void printMemoryTest(String tag) {
		printMemoryTest(tag, -1);
	}
	
	public static void printMemoryTest(String tag, int pause) {
		if(!MEMORY_PRINT_ENABLED) { return; }
		System.gc();
		Runtime r = Runtime.getRuntime();
		long free = r.freeMemory();
		long total = r.totalMemory();
		
		if(tag != null) {
			System.out.println("=== Memory Evaluation: " + tag + " ===");
		}
		
		System.out.println("Total: " +total + "\nFree: " + free);
		
		int chunk = 100;
		int lastSuccess = 100;
		
		int resolution = 10000;
		while(true) {
			System.gc();
			try {
				int newAmount  = lastSuccess + chunk;
				byte[] allocated = new byte[newAmount];
				lastSuccess = newAmount;
				chunk = chunk * 10;
			} catch(OutOfMemoryError oom) {
				chunk = chunk / 2;
				if(chunk < resolution) { 
					break;
				}
			}
		}
			

		int availPercent = (int)Math.floor((lastSuccess * 1.0 / total) * 100);
		int fragmentation = (int)Math.floor((lastSuccess * 1.0 / free) * 100);
		System.out.println("Usable Memory: " +lastSuccess + "\n" + availPercent + "% of available memory");
		System.out.println("Fragmentation: " + fragmentation + "%");
		
		if(pause != -1) {
			try {
				Thread.sleep(pause);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
