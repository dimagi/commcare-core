package org.javarosa.core.util.externalizable;

import org.javarosa.core.util.MD5;


/**
 * Default class hasher implementation.
 *
 * Based on in-house JavaRosa MD5'er
 *
 * Not super efficient on platforms where faster hashing
 * is available through other libraries
 *
 * @author ctsims
 */
public class DefaultHasher implements Hasher {
    private final static int CLASS_HASH_SIZE = 4;

    @Override
    public byte[] getClassHashValue(Class type) {
        byte[] hash = new byte[CLASS_HASH_SIZE];
        byte[] md5 = MD5.hash(type.getName().getBytes()); //add support for a salt, in case of collision?

        System.arraycopy(md5, 0, hash, 0, hash.length);

        byte[] badHash = new byte[]{0, 4, 78, 97};
        if (PrototypeFactory.compareHash(badHash, hash)) {
            System.out.println("BAD CLASS: " + type.getName());
        }

        return hash;
    }

    @Override
    public int getHashSize(){
        return CLASS_HASH_SIZE;
    }
}
