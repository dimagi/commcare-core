package org.commcare.xml;

import java.io.IOException;
import java.util.Vector;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

public class ResourceParser extends ElementParser<Resource> {
	
	public ResourceParser(KXmlParser parser) {
		super(parser);
	}

	public Resource parse() throws InvalidStructureException {
		if(!parser.getName().equals("resource")) {
			throw new InvalidStructureException(); 
		}
		try {
		String id = parser.getAttributeValue(null,"id");
		int version = parseInt(parser.getAttributeValue(null, "version"));
		
		Vector<ResourceLocation> locations = new Vector<ResourceLocation>();
		
		while(nextTagInBlock("resource")) {
			//New Location
			String sAuthority = parser.getAttributeValue(null,"authority");
			String location = parser.nextText();
			int authority = Resource.RESOURCE_AUTHORITY_REMOTE;
			if(sAuthority.toLowerCase().equals("local")) {
				authority = Resource.RESOURCE_AUTHORITY_LOCAL;
			}
			else if(sAuthority.toLowerCase().equals("remote")) {
				authority = Resource.RESOURCE_AUTHORITY_REMOTE;
			}
			
			locations.addElement(new ResourceLocation(authority, location));
		}
		
		return new Resource(version, id, locations);
		
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			throw new InvalidStructureException(); 
		} catch (IOException e) {
			e.printStackTrace();
			throw new InvalidStructureException(); 
		}
	}
}
