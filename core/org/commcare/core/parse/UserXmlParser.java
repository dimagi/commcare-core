package org.commcare.core.parse;

import org.commcare.data.xml.TransactionParser;
import org.javarosa.core.model.User;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 *
 * @author ctsims
 */
public class UserXmlParser extends TransactionParser<User> {

    private IStorageUtilityIndexed<User> storage;

    public UserXmlParser(KXmlParser parser) {
        super(parser);
    }

    public UserXmlParser(KXmlParser parser, IStorageUtilityIndexed<User> storage) {
        super(parser);
        this.storage = storage;
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
        DateUtils.parseDateTime(dateModified);

        User u = retrieve(uuid);

        if (u == null) {
            u = new User(username, passwordHash, uuid);
        } else {
            if (passwordHash != null && !passwordHash.equals(u.getPasswordHash())) {
                u.setPassword(passwordHash);
            }
        }

        //Now look for optional components
        while (this.nextTagInBlock("registration")) {

            String tag = parser.getName().toLowerCase();

            if (tag.equals("registering_phone_id")) {
                parser.nextText();
            } else if (tag.equals("token")) {
                parser.nextText();
            } else if (tag.equals("user_data")) {
                while (this.nextTagInBlock("user_data")) {
                    this.checkNode("data");

                    String key = this.parser.getAttributeValue(null, "key");
                    String value = this.parser.nextText();

                    u.setProperty(key, value);
                }

                //This should be the last block in the registration stuff...
                break;
            } else {
                throw new InvalidStructureException("Unrecognized tag in user registraiton data: " + tag, parser);
            }
        }

        addCustomData(u);

        if (storage != null) {
            commit(u);
        }
        return u;
    }

    public void addCustomData(User u){
        //Don't do anything in base class
    }

    @Override
    protected void commit(User parsed) throws IOException {
        try {
            storage().write(parsed);
        } catch (StorageFullException e) {
            e.printStackTrace();
            throw new IOException("Storage full while writing case!");
        }
    }

    public User retrieve(String entityId) {
        IStorageUtilityIndexed storage = storage();
        try {
            return (User)storage.getRecordForValue(User.META_UID, entityId);
        } catch (NoSuchElementException nsee) {
            return null;
        }
    }

    public IStorageUtilityIndexed storage() {
        return storage;
    }
}
