/**
 * 
 */
package org.commcare.applogic;

import java.io.IOException;

import org.commcare.data.xml.DataModelPullParser;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.xml.CaseXmlParser;
import org.commcare.xml.UserXmlParser;
import org.commcare.xml.util.InvalidStructureException;
import org.javarosa.core.api.State;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.util.TrivialTransitions;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public abstract class DataRestoreState implements State, TrivialTransitions {

	public DataRestoreState() {
		
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.core.api.State#start()
	 */
	public void start() {
		String URI = "jr://resource/data.xml";
		try {
			DataModelPullParser parser = new DataModelPullParser(ReferenceManager._().DeriveReference(URI).getStream(), 
					new TransactionParserFactory() {
						public TransactionParser getParser(String name,
								String namespace, KXmlParser parser) {
							name = name.toLowerCase();
							if("case".equals(name)) {
								return new CaseXmlParser(parser);
							} else if("registration".equals(name)){
								return new UserXmlParser(parser);
							}
							return null;
						}
			});
			
			if(!parser.parse().booleanValue()) {
				for(String s : parser.getParseErrors()) {
					System.out.println(s);
				}
			}
			this.done();
		} catch (InvalidStructureException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidReferenceException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
	}

}
