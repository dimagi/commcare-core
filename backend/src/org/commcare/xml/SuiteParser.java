/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import org.commcare.resources.model.LocaleFileResourceInitializer;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.XFormResourceInitializer;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Suite;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class SuiteParser extends ElementParser<Suite>  {
	
	Suite suite;
	ResourceTable table;
	String resourceGuid;
	
	public SuiteParser(InputStream suiteStream, ResourceTable table, String resourceGuid) {
		super(suiteStream);
		this.table = table;
		this.resourceGuid = resourceGuid;
	}
	
	public SuiteParser(KXmlParser parser, ResourceTable table, String resourceGuid) {
		super(parser);
		this.table = table;
		this.resourceGuid = resourceGuid;
	}
	
	public Suite parse() throws InvalidStructureException {
		if(!parser.getName().toLowerCase().equals("suite")) {
			throw new InvalidStructureException();
		}
		
    	
		String sVersion = parser.getAttributeValue(null, "version");
		int version = Integer.parseInt(sVersion);
		Hashtable<String, Detail> details = new Hashtable<String, Detail>();
		Hashtable<String, Entry> entries = new Hashtable<String, Entry>();
		
		try {
			
			//Now that we've covered being inside of a suite, 
			//start traversing.
			parser.next();
			
	        int eventType;
			eventType = parser.getEventType();
        do {
            if(eventType == KXmlParser.END_DOCUMENT) {
            } else if(eventType == KXmlParser.START_TAG) {
                if(parser.getName().toLowerCase().equals("entry")) {
                	Entry e = new EntryParser(parser).parse();
                	entries.put(e.getCommandId(), e);
                } else if(parser.getName().toLowerCase().equals("view")) {
                	Entry e = new ViewParser(parser).parse();
                	entries.put(e.getCommandId(), e);
                } else if(parser.getName().toLowerCase().equals("locale")) {
                	String localeKey = parser.getAttributeValue(null, "language");
                	//resource def
                	parser.nextTag();
                	Resource r = new ResourceParser(parser).parse();
                	table.addResource(r, new LocaleFileResourceInitializer(localeKey), resourceGuid);
                } else if(parser.getName().toLowerCase().equals("xform")) {
                	//skip xform stuff for now
                	parser.nextTag();
                	Resource r = new ResourceParser(parser).parse();
                	table.addResource(r, new XFormResourceInitializer(), resourceGuid);
                } else if(parser.getName().toLowerCase().equals("detail")) {
                	Detail d = new DetailParser(parser).parse();
                	details.put(d.getId(), d);
                } else {
                	System.out.println("Unrecognized Tag: " + parser.getName());
                }
            } else if(eventType == KXmlParser.END_TAG) {
                //we shouldn't ever get this I don't believe, maybe on the last node?
            } else if(eventType == KXmlParser.TEXT) {
                //Shouldn't ever get this (Delete the if, if so).
            }
            eventType = parser.next();
        } while (eventType != KXmlParser.END_DOCUMENT);
				
		suite = new Suite(table, version, details, entries);
		return suite;
		
        
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new InvalidStructureException();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new InvalidStructureException();
		}
	}

}
