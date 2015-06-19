package org.commcare.util.mocks;

import org.javarosa.core.util.MD5;
import org.javarosa.core.util.externalizable.Hasher;
import org.javarosa.core.util.externalizable.PrototypeFactory;


/**
 * Default class hasher implementation.
 * 
 * Based on in-house JavaRosa MD5'er 
 * 
 * Not super efficient on platforms where faster hashing
 * is available through other libraries
 * 
 * @author ctsims
 *
 */
public class DefaultHasher implements Hasher{
    public final static int CLASS_HASH_SIZE = 4;
    
    @Override
    public byte[] getClassHashValue(Class type) {
        byte[] hash = new byte[CLASS_HASH_SIZE];
        byte[] md5 = MD5.hash(type.getName().getBytes()); //add support for a salt, in case of collision?

        for (int i = 0; i < hash.length; i++)
            hash[i] = md5[i];
        byte[] badHash = new byte[]{0, 4, 78, 97};
        if (PrototypeFactory.compareHash(badHash, hash)) {
            System.out.println("BAD CLASS: " + type.getName());
        }

        return hash;

    }
}
