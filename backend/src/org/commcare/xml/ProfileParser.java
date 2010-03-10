/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;
import java.io.InputStream;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.installers.LoginImageInstaller;
import org.commcare.resources.model.installers.SuiteInstaller;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Root;
import org.commcare.xml.util.InvalidStructureException;
import org.javarosa.core.services.storage.StorageFullException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class ProfileParser extends ElementParser<Profile> {
	
	ResourceTable table;
	String resourceId;
	int initialResourceStatus;

	public ProfileParser(InputStream suiteStream, ResourceTable table, String resourceId, int initialResourceStatus) {
		super(suiteStream);
		this.table = table;
		this.resourceId = resourceId;
		this.initialResourceStatus = initialResourceStatus;
	}

	public Profile parse() throws InvalidStructureException {
		if (!parser.getName().toLowerCase().equals("profile")) {
			throw new InvalidStructureException();
		}

		String sVersion = parser.getAttributeValue(null, "version");
		int version = parseInt(sVersion);

		String authRef = parser.getAttributeValue(null, "update");
		
		Profile profile = new Profile(version, authRef);
		try {

			// Now that we've covered being inside of the profile,
			// start traversing.
			parser.next();

			int eventType;
			eventType = parser.getEventType();
			do {
				if (eventType == KXmlParser.END_DOCUMENT) {
				} else if (eventType == KXmlParser.START_TAG) {
					if (parser.getName().toLowerCase().equals("property")) {
						String key = parser.getAttributeValue(null,"key");
						String value = parser.getAttributeValue(null,"value");
						String force = parser.getAttributeValue(null,"force");
						if(force != null) {
							if("true".equals(force.toLowerCase())) {
								
							}
						}
					} else if (parser.getName().toLowerCase().equals("root")) {
						Root root = new RootParser(this.parser).parse();
						
					} else if (parser.getName().toLowerCase().equals("login")) {
						//Get the resource block or fail out
						if(!nextTagInBlock("login")) {
							throw new InvalidStructureException();
						}
						Resource resource = new ResourceParser(parser).parse();
						table.addResource(resource, new LoginImageInstaller(), resourceId,initialResourceStatus);
					} else if (parser.getName().toLowerCase().equals("features")) {
						while(nextTagInBlock("features")) {
							String tag = parser.getName().toLowerCase();
							String active = parser.getAttributeValue(null,"active");
							boolean isActive = false;
							if (active != null && active.toLowerCase().equals("true")) {
								isActive = true;
							}
							if (tag.equals("checkoff")) {
								// nothing
							} else if (tag.equals("reminders")) {
								if(nextTagInBlock("reminders")) {
									checkNode("time");
									String reminderTime = parser.nextText();
									try {
										int days = Integer.parseInt(reminderTime);
									} catch(NumberFormatException nfe) {
										throw new InvalidStructureException();
									}
								}
							} else if (tag.equals("package")) {
								//nothing (yet)
							}
							
							//TODO: set feature activation in profile
						}
					} else if (parser.getName().toLowerCase().equals("suite")) {
						// Get the resource block or fail out
						if (!nextTagInBlock("suite")) {
							throw new InvalidStructureException();
						}
						Resource resource = new ResourceParser(parser).parse();
						
						//TODO: Possibly add a real parent reference if we decide these go in the table
						table.addResource(resource, new SuiteInstaller(), resourceId, initialResourceStatus);
					} else {
						System.out.println("Unrecognized Tag: "
								+ parser.getName());
					}
				} else if (eventType == KXmlParser.END_TAG) {
					// we shouldn't ever get this I don't believe, maybe on the
					// last node?
				} else if (eventType == KXmlParser.TEXT) {
					// Shouldn't ever get this (Delete the if, if so).
				}
				eventType = parser.next();
			} while (eventType != KXmlParser.END_DOCUMENT);

		return profile;

		} catch (XmlPullParserException e) {
			e.printStackTrace();
			throw new InvalidStructureException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new InvalidStructureException();
		} catch (StorageFullException e) {
			e.printStackTrace();
			throw new InvalidStructureException();
		} 
	}

}
