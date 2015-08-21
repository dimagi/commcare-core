package org.commcare.xml;

import org.commcare.core.parse.UserXmlParser;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.model.User;
import org.kxml2.io.KXmlParser;

public class J2MEUserXmlParser extends UserXmlParser {

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
