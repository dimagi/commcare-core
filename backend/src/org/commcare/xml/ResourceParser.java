package org.commcare.xml;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Vector;

public class ResourceParser extends ElementParser<Resource> {

    final int maximumAuthority;

    public ResourceParser(KXmlParser parser, int maximumAuthority) {
        super(parser);
        this.maximumAuthority = maximumAuthority;
    }

    public Resource parse() throws InvalidStructureException, IOException, XmlPullParserException {
        checkNode("resource");

        String id = parser.getAttributeValue(null, "id");
        int version = parseInt(parser.getAttributeValue(null, "version"));

        String descriptor = parser.getAttributeValue(null, "descriptor");

        Vector<ResourceLocation> locations = new Vector<>();

        while (nextTagInBlock("resource")) {
            //New Location
            String sAuthority = parser.getAttributeValue(null, "authority");
            String location = parser.nextText();
            int authority = Resource.RESOURCE_AUTHORITY_REMOTE;
            if (sAuthority.toLowerCase().equals("local")) {
                authority = Resource.RESOURCE_AUTHORITY_LOCAL;
            } else if (sAuthority.toLowerCase().equals("remote")) {
                authority = Resource.RESOURCE_AUTHORITY_REMOTE;
            }
            //Don't use any authorities which are outside of the scope of the maximum
            if (authority >= maximumAuthority) {
                locations.addElement(new ResourceLocation(authority, location));
            }
        }

        return new Resource(version, id, locations, descriptor);
    }
}
