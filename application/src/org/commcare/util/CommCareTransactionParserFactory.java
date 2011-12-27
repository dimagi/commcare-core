/**
 * 
 */
package org.commcare.util;

import java.io.IOException;

import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.xml.CaseXmlParser;
import org.commcare.xml.FixtureXmlParser;
import org.commcare.xml.UserXmlParser;
import org.commcare.xml.util.InvalidStructureException;
import org.javarosa.core.services.Logger;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * The CommCare Transaction Parser Factory (whew!) wraps all of the current 
 * transactions that CommCare knows about, and provides the appropriate hooks for
 * parsing through XML and dispatching the right handler for each transaction.
 * 
 * It should be the central point of processing for transactions (eliminating the
 * use of the old datamodel based processors) and should be used in any situation where
 * a transaction is expected to be present.
 * 
 * It is expected to behave more or less as a black box, in that it directly creates/modifies
 * the data models on the system, rather than producing them for another layer or processing.
 * 
 * @author ctsims
 *
 */
public class CommCareTransactionParserFactory implements TransactionParserFactory {
	
	private int[] caseTallies;
	private String restoreId;
	private boolean tolerant;
	private String message;
	
	/**
	 * Creates a new factory for processing incoming XML.
	 * @param tolerant True if processing should fail in the event of conflicting data,
	 * false if processing should proceed as long as it is possible.
	 */
	public CommCareTransactionParserFactory(boolean tolerant) {
		restoreId = null;
		caseTallies = new int[3];
		this.tolerant = tolerant;
	}

	/*
	 * (non-Javadoc)
	 * @see org.commcare.data.xml.TransactionParserFactory#getParser(java.lang.String, java.lang.String, org.kxml2.io.KXmlParser)
	 */
	public TransactionParser getParser(String name, String namespace, KXmlParser parser) {
		if(name.toLowerCase().equals("case")) {
			return new CaseXmlParser(parser, caseTallies, tolerant);
		} else if(name.toLowerCase().equals("registration")) {
			//TODO: It's possible we want to do the restoreID thing after signalling success, actually. If the 
			//restore gets cut off, we don't want to be re-sending the token, since it implies that it worked.
			return new UserXmlParser(parser, restoreId);
		} else if(name.toLowerCase().equals("message")) {
			message = parser.getText();
		} else if (name.equalsIgnoreCase("Sync")) {
			return new TransactionParser<String> (parser, "Sync", null) {
				public void commit(String parsed) throws IOException {
					//do nothing
				}
				
				public String parse() throws XmlPullParserException, IOException, InvalidStructureException {
					if(this.nextTagInBlock("Sync")){
						this.checkNode("restore_id");
						String newId = parser.nextText().trim();
						if(restoreId != null) {
							Logger.log("TRANSACTION","Warning: Multiple restore ID's seen:" + restoreId + "," + newId);
						}
						restoreId = newId;
						return restoreId;
					} else {
						throw new InvalidStructureException("<Sync> block missing <restore_id>", this.parser);
					}
				}
			};
		} else if(name.toLowerCase().equals("fixture")) {
			return new FixtureXmlParser(parser);
		}
		return null;
	}
	
	/**
	 * @return An int[3] array containing a count of Cases
	 * int[0]: created
	 * int[1]: updated
	 * int[2]: closed
	 * 
	 * after processing has completed.
	 */
	public int[] getCaseTallies() {
		return caseTallies;
	}
	
	/**
	 * @return After processing is completed, if a restore ID was present in the payload
	 * it will be returned here. 
	 */
	public String getRestoreId() {
		return restoreId;
	}
	
	/**
	 * @return After processing is completed, if a message to the user was present, it
	 * will be returned here.
	 */
	public String getResponseMessage() {
		return message;
	}
}
