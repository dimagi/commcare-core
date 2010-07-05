/**
 * 
 */
package org.commcare.data.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.commcare.xml.ElementParser;
import org.commcare.xml.util.InvalidStructureException;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.services.Logger;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A DataModelPullParser pulls together the parsing of
 * different data models in order to be able to perform
 * a master update/restore of remote data.
 * 
 * 
 * @author ctsims
 *
 */
public class DataModelPullParser extends ElementParser<Boolean> {
	
	Vector<String> errors;
	
	TransactionParserFactory factory;
	
	boolean failfast;
	
	public DataModelPullParser(InputStream is, TransactionParserFactory factory) throws InvalidStructureException, IOException {
		this(is,factory,false);
	}
	
	public DataModelPullParser(InputStream is, TransactionParserFactory factory, boolean failfast) throws InvalidStructureException, IOException {
		super(is);
		this.failfast = failfast;
		this.factory = factory;
		errors = new Vector<String>();
	}

	public Boolean parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
		String rootName = parser.getName();
		//Here we'll go through in search of CommCare data models and parse
		//them using the appropriate CommCare Model data parser.
		
		//Go through each child of the root element
		while(this.nextTagInBlock(rootName)) {
			
			String name = parser.getName();
			String namespace = parser.getNamespace();
			
			int depth = parser.getDepth();
			
			TransactionParser transaction = factory.getParser(name,namespace,parser);
			if(transaction == null) {
				//nothing to be done for this element, need to step over it
				this.skipBlock(name);
				//TODO: Don't skip over, jump in...
			} else {
				try{
					transaction.parse();
				} catch(InvalidStructureException ise) {
					deal(ise, depth, name);
				}
			}
		}
		
		if(errors.size() == 0) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}
	
	private void deal(InvalidStructureException e, int depth, String parentTag) throws InvalidStructureException, XmlPullParserException, IOException {
		if(failfast) {
			throw e;
		} else{
			String message = e.getMessage();
			errors.addElement(message);
			System.out.println("error: " + e.getMessage());
			this.skipBlock(parentTag);
		}
	}
	
	public String[] getParseErrors() {
		String[] errorBuf = new String[errors.size()];
		for(int i = 0 ; i < errorBuf.length ; ++i) {
			errorBuf[i] = errors.elementAt(i);
		}
		return errorBuf;
	}
}
