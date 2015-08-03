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

    public byte[] getClassHashValue(Class type) {

        byte[] hash = new byte[this.getHashSize()];
        byte[] md5 = getHash(type); //add support for a salt, in case of collision?

        for(int i=0; i< hash.length && i<md5.length; i++){
            hash[i] = md5[i];
        }

        byte[] badHash = new byte[]{0, 4, 78, 97};
        if (PrototypeFactory.compareHash(badHash, hash)) {
            System.out.println("BAD CLASS: " + type.getName());
        }

        return hash;
    }

    public byte[] getHash(Class c){
        return MD5.hash(c.getName().getBytes());
    }

    public int getHashSize(){
        return CLASS_HASH_SIZE;
    }
}
