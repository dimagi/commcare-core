/**
 * 
 */
package org.commcare.util;

import java.io.IOException;

import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.xml.CaseXmlParser;
import org.commcare.xml.UserXmlParser;
import org.javarosa.core.services.Logger;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class CommCareTransactionParserFactory implements TransactionParserFactory {
	
	private int[] caseTallies;
	private String restoreId;
	private boolean tolerant;
	private String message;
	
	public CommCareTransactionParserFactory(boolean tolerant) {
		restoreId = null;
		caseTallies = new int[3];
		this.tolerant = tolerant;
	}

	public TransactionParser getParser(String name, String namespace, KXmlParser parser) {
		if(name.toLowerCase().equals("case")) {
			return new CaseXmlParser(parser, caseTallies, tolerant);
		} else if(name.toLowerCase().equals("registration")) {
			return new UserXmlParser(parser);
		} else if(name.toLowerCase().equals("message")) {
			message = parser.getText();
		} else if (name.equalsIgnoreCase("restore_id")) {
			return new TransactionParser<String> (parser, "restore_id", null) {
				public void commit(String parsed) throws IOException {
					//do nothing
				}
				
				public String parse() throws XmlPullParserException, IOException {
					String newId = parser.nextText().trim();
					if(restoreId != null) {
						Logger.log("TRANSACTION","Warning: Multiple restore ID's seen:" + restoreId + "," + newId);
					}
					restoreId = newId;
					return restoreId;
				}
			};
		}
		return null;
	}
	
	public int[] getCaseTallies() {
		return caseTallies;
	}
	
	public String getRestoreId() {
		return restoreId;
	}
	
	public String getResponseMessage() {
		return message;
	}
}
