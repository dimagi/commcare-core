package org.commcare.api.util;

import org.javarosa.core.util.externalizable.Hasher;


/**
 * Most simple possible "hasher" - just gets the byte representation of the hash name
 *
 * @author wspride
 */
public class NameHasher implements Hasher {

    private final static int CLASS_HASH_SIZE = 32;

    @Override
    public byte[] getClassHashValue(Class type) {

        byte[] nameBytes = type.getName().getBytes();
        byte[] hash = new byte[CLASS_HASH_SIZE];
        for(int i=0; i<nameBytes.length && i<CLASS_HASH_SIZE; i++){
            byte b = nameBytes[i];
            hash[i] = b;
        }
        return hash;
    }

    public int getHashSize(){
        return CLASS_HASH_SIZE;
    }
}
