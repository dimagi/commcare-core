package org.javarosa.core.api;

import org.javarosa.core.util.externalizable.DefaultHasher;


/**
 * Most simple possible "hasher" - just gets the byte representation of the hash name
 *
 * @author wspride
 */
public class NameHasher extends DefaultHasher {

    public final static int CLASS_HASH_SIZE = 32;

    public byte[] doHash(Class c){
        return c.getName().getBytes();
    }

    public int getHashSize(){
        return CLASS_HASH_SIZE;
    }
}
