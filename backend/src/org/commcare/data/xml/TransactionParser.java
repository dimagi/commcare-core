/**
 * 
 */
package org.commcare.data.xml;

import java.io.IOException;

import org.commcare.xml.ElementParser;
import org.kxml2.io.KXmlParser;

/**
 * @author ctsims
 *
 */
public abstract class TransactionParser<T> extends ElementParser<T> {

	String name;
	String namespace;
	
	
	public TransactionParser(KXmlParser parser, String name, String namespace) {
		super(parser);
	}
	
	public boolean parses(String name, String namespace) {
		if(name.toLowerCase().equals(this.name)) {
			return true;
		}
		return false;
	}
	
	public abstract void commit(T parsed) throws IOException;
}
