/**
 * 
 */
package org.javarosa.xform.util;

import org.kxml2.io.KXmlParser;

/**
 * @author ctsims
 *
 */
public class InterningKXmlParser extends KXmlParser{
	
	public InterningKXmlParser() {
		super();
	}
	public void release() {
		//Anything?
	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getAttributeName(int)
	 */
	public String getAttributeName(int arg0) {
		return super.getAttributeName(arg0).intern();
		
	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getAttributeNamespace(int)
	 */
	public String getAttributeNamespace(int arg0) {
		return super.getAttributeNamespace(arg0).intern();

	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getAttributePrefix(int)
	 */
	public String getAttributePrefix(int arg0) {
		return super.getAttributePrefix(arg0).intern();
	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getAttributeValue(int)
	 */
	public String getAttributeValue(int arg0) {
		return super.getAttributeValue(arg0).intern();

	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getNamespace(java.lang.String)
	 */
	public String getNamespace(String arg0) {
		return super.getNamespace(arg0).intern();

	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getNamespaceUri(int)
	 */
	public String getNamespaceUri(int arg0) {
		return super.getNamespaceUri(arg0).intern();
	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getText()
	 */
	public String getText() {
		return super.getText().intern();

	}
	
	public String getName() {
		return super.getName().intern();
	}
}
