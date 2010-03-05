/**
 * 
 */
package org.commcare.reference;

import java.util.Vector;


/**
 * @author ctsims
 *
 */
public class ReferenceManager {
	
	private static Vector<Reference> referenceRoots = new Vector<Reference>();
	
	public static Reference[] getRoots() {
		Reference[] roots = new Reference[referenceRoots.size()];
		referenceRoots.copyInto(roots);
		return roots;
	}
	
	public static Reference DeriveReference(String value) throws InvalidReferenceException{
		return DeriveReference(null, value);
	}
	
	public static Reference DeriveReference(Reference context, String value) throws InvalidReferenceException{
		if(value.startsWith("jr://")) {
			return createJavaRosaReference(value);
		} else if(isRelative(value)) {
			//Local Reference.
			if(context != null) {
				return context.contextualize(value);
			} else {
				throw new RuntimeException("Attempted to retrieve local reference with no context");
			}
		} else if(value.startsWith("http://")){
			return new HttpReference(value, value);
		} else if(value.startsWith("https://")){
			return new HttpReference(value, value);
		}
			else {
			//Now what? Ask for a generic connector? Check for http/https/bt/etc?
			return null;
		}
	}
	
	public static boolean isRelative(String URI) {
		if(URI.startsWith("./")) {
			return true;
		}
		return false;
	}
	
	public static String contextualizeURI(String parent, String child) {
		if(child.startsWith("./")) {
			child = child.substring(2);
		}
		
		if(parent.lastIndexOf('/') == -1) {
			parent = "";
		} else {
			parent = parent.substring(0,parent.lastIndexOf('/'));
		}
		
		return parent + "/" + child;
	}
	
	public static Reference createJavaRosaReference(String reference) throws InvalidReferenceException{
		//Strip out jr://
		String uri = reference.substring(5);
		if(uri.startsWith("resource/")) {
			ResourceReference r = new ResourceReference(reference, uri.substring(8));
			return r;
		} else {
			throw new InvalidReferenceException("No valid javarosa reference could be created for the string \"" + reference + "\"", reference);
		}
	}
}
