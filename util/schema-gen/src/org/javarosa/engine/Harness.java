/**
 * 
 */
package org.javarosa.engine;

import java.io.FileNotFoundException;

import org.javarosa.engine.models.Session;

/**
 * @author ctsims
 *
 */
public class Harness {
	
	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println("Usage: java -jar Player");
		}
		if(args.length == 1) {
			XFormPlayer xfp = new XFormPlayer(System.in, System.out);
			//XFormPlayer xfp = new XFormPlayer(System.console(), System.out);
			try {
				xfp.start(args[0]);
				Session s = xfp.environment.getSessionRecording();
			} catch (FileNotFoundException e) {
				System.out.println("File not found at " + args[0]+ "!!!!");
			}
		} else if(args.length == 2) {
			XFormPlayer xfp = new XFormPlayer(System.in, System.out);
			//XFormPlayer xfp = new XFormPlayer(System.console(), System.out);
			try {
				xfp.start(args[0]);
				Session s = xfp.environment.getSessionRecording();
				xfp = new XFormPlayer(System.in, System.out);
				xfp.start(args[1], s);
			} catch (FileNotFoundException e) {
				System.out.println("File not found at " + args[0]+ "!!!!");
			}
		}
		System.exit(0);
	}
	
}
