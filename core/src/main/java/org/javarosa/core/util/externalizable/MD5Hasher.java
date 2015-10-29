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
public class MD5Hasher extends Hasher {
    private final static int CLASS_HASH_SIZE = 4;

    @Override
    public byte[] getHash(Class c){
        return MD5.hash(c.getName().getBytes());
    }

    @Override
    public int getHashSize(){
        return CLASS_HASH_SIZE;
    }
}
