/**
 * 
 */
package org.commcare.util;

/**
 * @author ctsims
 *
 */
public class Harness {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length < 1) {
			printformat();
			System.exit(-1);
		} 
		if(args[0].equals("validate")) {
			if(args.length < 2) {
				printvalidateformat();
				System.exit(-1);
			}
			CommCareConfigEngine engine = new CommCareConfigEngine(System.out);
			
			//TODO: check arg for whether it's a local or global file resource and
			//make sure it's absolute
			
			engine.addLocalFileResource(args[1]);
			engine.resolveTable();
			engine.validateResources();
			engine.describeApplication();
			System.exit(0);
		}
	}
	
	private static void printformat() {
		System.out.println("Usage: java -jar thejar.jar [validate|otherstuff]");
	}
	
	private static void printvalidateformat() {
		System.out.println("Usage: java -jar thejar.jar validate inputfile.xml [otherfiles.xml ....]");
	}

}
