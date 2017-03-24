package org.commcare.xml;

import org.commcare.suite.model.AppAvailableForInstall;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by amstone326 on 2/3/17.
 */
public class AvailableAppsParser extends ElementParser<List<AppAvailableForInstall>> {

    private static final String APPS_TAG = "apps";
    private static final String APP_TAG = "app";
    private static final String DOMAIN_TAG = "domain";
    private static final String APP_NAME_TAG = "name";
    private static final String APP_VERSION_TAG = "version";
    private static final String PROFILE_REF_TAG = "profile";
    private static final String MEDIA_PROFILE_REF_TAG = "media-profile";
    private static final String ENVIRONMENT_TAG = "environment";
    private static final String PROD_VALUE = "Production";

    public AvailableAppsParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public List<AppAvailableForInstall> parse() throws InvalidStructureException, IOException,
            XmlPullParserException, UnfullfilledRequirementsException {
        checkNode(APPS_TAG);
        List<AppAvailableForInstall> appsList = new ArrayList<>();

        parser.next();
        int eventType = parser.getEventType();
        do {
            if (eventType == KXmlParser.START_TAG) {
                String tagName = parser.getName().toLowerCase();
                if (APP_TAG.equals(tagName)) {
                    String domain = parser.getAttributeValue(null, DOMAIN_TAG);
                    String appName = parser.getAttributeValue(null, APP_NAME_TAG);
                    String appVersion = parser.getAttributeValue(null, APP_VERSION_TAG);
                    boolean isOnProd = PROD_VALUE.equals(parser.getAttributeValue(null, ENVIRONMENT_TAG));
                    String profileRef = parser.getAttributeValue(null, PROFILE_REF_TAG);
                    String mediaProfileRef = parser.getAttributeValue(null, MEDIA_PROFILE_REF_TAG);
                    appsList.add(new AppAvailableForInstall(domain, appName, appVersion, isOnProd, profileRef, mediaProfileRef));
                }
            }
            eventType = parser.next();
        } while (eventType != KXmlParser.END_DOCUMENT);

        return appsList;
    }

}
