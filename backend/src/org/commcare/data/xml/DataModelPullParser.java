/**
 * 
 */
package org.commcare.data.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.commcare.resources.model.CommCareOTARestoreListener;
import org.commcare.xml.ElementParser;
import org.commcare.xml.util.InvalidStructureException;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.log.WrappedException;
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
public class DataModelPullParser extends ElementParser<Boolean>{
	
	Vector<String> errors;
	
	TransactionParserFactory factory;
	
	boolean failfast;
	boolean deep;
	
	InputStream is;
	
	CommCareOTARestoreListener rListener;
	
	public DataModelPullParser(InputStream is, TransactionParserFactory factory) throws InvalidStructureException, IOException {
		this(is, factory, false);
	}
	
	public DataModelPullParser(InputStream is, TransactionParserFactory factory, CommCareOTARestoreListener rl) throws InvalidStructureException, IOException {
		this(is, factory, false);
		this.rListener = rl;
	}
	
	public DataModelPullParser(InputStream is, TransactionParserFactory factory, boolean deep) throws InvalidStructureException, IOException {
		this(is,factory,false, deep);
	}
	
	public DataModelPullParser(InputStream is, TransactionParserFactory factory, boolean failfast, boolean deep) throws InvalidStructureException, IOException {
		super(is);
		this.is = is;
		this.failfast = failfast;
		this.factory = factory;
		errors = new Vector<String>();
		this.deep = deep;
	}

	public Boolean parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
		try {
			
			System.out.println("parsing in datamodelpullparser");
			
			String rootName = parser.getName();
			
			String itemString = parser.getAttributeValue(null, "items");
			
			int itemNumber = -1;
			
			if(itemString != null) {
				
				System.out.println("item string is: " + itemString);
				
				try{
					itemNumber = Integer.parseInt(itemString);
				}catch(NumberFormatException e){
					itemNumber = 0;
				}
				rListener.setTotalForms(itemNumber);
				//throw new InvalidStructureException("<item> block with no item_id attribute.", this.parser);
			}
			//Here we'll go through in search of CommCare data models and parse
			//them using the appropriate CommCare Model data parser.
			
			//Go through each child of the root element
			parseBlock(rootName);
		} finally {
			//kxmlparser might close the stream, but we can't be sure, especially if
			//we bail early due to schema errors
			try {
				is.close();
			} catch (IOException ioe) {
				//swallow
			}
		}
		
		if(errors.size() == 0) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}
	
	private void parseBlock(String root)  throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
		int parsedCounter = 0;
		while(this.nextTagInBlock(root)) {
			
			System.out.println("!!! - Iterating in parseBlock");
			
			if(listenerSet()){
				System.out.println("Calling onUpdate!!!");
				rListener.onUpdate(parsedCounter);
				parsedCounter++;
			}
			
			String name = parser.getName();
			String namespace = parser.getNamespace();
			
			int depth = parser.getDepth();
			if(name == null) {
				continue;
			}
			
			TransactionParser transaction = factory.getParser(name,namespace,parser);
			if(transaction == null) {
				//nothing to be done for this element, recurse?
				if(deep) {
					parseBlock(name);
				} else {
					this.skipBlock(name);
				}
			} else {
				try{
					transaction.parse();
				} catch(Exception e) {
					e.printStackTrace();
					deal(e, depth, name);
				}
			}
		}
	}
	
	private void deal(Exception e, int depth, String parentTag) throws XmlPullParserException, IOException {
		errors.addElement(WrappedException.printException(e));
		this.skipBlock(parentTag);
		
		if(failfast) {
			throw new WrappedException(e);
		}
	}
	
	public String[] getParseErrors() {
		String[] errorBuf = new String[errors.size()];
		for(int i = 0 ; i < errorBuf.length ; ++i) {
			errorBuf[i] = errors.elementAt(i);
		}
		return errorBuf;
	}
	
	public boolean listenerSet(){
		if(rListener==null){return false;}
		return true;
	}
}
