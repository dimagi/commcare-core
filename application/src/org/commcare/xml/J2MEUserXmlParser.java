package org.commcare.xml;

import org.commcare.core.parse.UserXmlParser;
import org.commcare.data.xml.TransactionParser;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.commcare.suite.model.User;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Date;
import java.util.NoSuchElementException;

public class J2MEUserXmlParser extends UserXmlParser {

    IStorageUtilityIndexed storage;
    String syncToken;

    public J2MEUserXmlParser(KXmlParser parser, IStorageUtilityIndexed<User> storage, String syncToken) {
        super(parser, storage);
        this.syncToken = syncToken;
    }

    public void addCustomData(User u){
        if(syncToken != null){
            u.setLastSyncToken(syncToken);
        }
    }

}
