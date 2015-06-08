package org.commcare.xml;

import java.io.IOException;
import java.util.Date;
import java.util.NoSuchElementException;

import org.commcare.data.xml.TransactionParser;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.user.model.User;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

public class UserXmlParser extends TransactionParser<User> {

    IStorageUtilityIndexed storage;
    String syncToken;

    public UserXmlParser(KXmlParser parser) {
        this(parser, null);
    }

    public UserXmlParser(KXmlParser parser, String syncToken) {
        super(parser);
        this.syncToken = syncToken;
    }

    public User parse() throws InvalidStructureException, IOException, XmlPullParserException {
        this.checkNode("registration");

        //parse (with verification) the next tag
        this.nextTag("username");
        String username = parser.nextText();

        this.nextTag("password");
        String passwordHash = parser.nextText();

        this.nextTag("uuid");
        String uuid = parser.nextText();

        this.nextTag("date");
        String dateModified = parser.nextText();
        Date modified = DateUtils.parseDateTime(dateModified);

        User u = retrieve(uuid);

        if(u == null) {
            u = new User(username, passwordHash, uuid);
        } else {
            u.setPassword(passwordHash);
            u.setUsername(username);
        }

        //Now look for optional components
        while(this.nextTagInBlock("registration")) {

            String tag = parser.getName().toLowerCase();

            if(tag.equals("registering_phone_id")) {
                String phoneid = parser.nextText();
            } else if(tag.equals("token")) {
                String token = parser.nextText();
            } else if(tag.equals("user_data")) {
                while(this.nextTagInBlock("user_data")) {
                    this.checkNode("data");

                    String key = this.parser.getAttributeValue(null, "key");
                    String value = this.parser.nextText();

                    u.setProperty(key, value);
                }

                //This should be the last block in the registration stuff...
                break;
            } else {
                throw new InvalidStructureException("Unrecognized tag in user registraiton data: " + tag,parser);
            }
        }

        //If this user's being restored as part of a sync action, we want the phone to know what the root of that action was!
        if(syncToken != null) {
            u.setLastSyncToken(syncToken);
        }
        commit(u);
        return u;
    }

    public void commit(User parsed) throws IOException {
        try {
            storage().write(parsed);
        } catch (StorageFullException e) {
            e.printStackTrace();
            throw new IOException("Storage full while writing case!");
        }
    }

    public User retrieve(String entityId) {
        IStorageUtilityIndexed storage = storage();
        try{
            return (User)storage.getRecordForValue(User.META_UID, entityId);
        } catch(NoSuchElementException nsee) {
            return null;
        }
    }

    public IStorageUtilityIndexed storage() {
        if(storage == null) {
            storage = (IStorageUtilityIndexed)StorageManager.getStorage(User.STORAGE_KEY);
        }
        return storage;
    }

}
