package org.javarosa.core.api;

import org.javarosa.core.util.externalizable.DefaultHasher;


/**
 * Most simple possible "hasher" - just gets the byte representation of the hash name
 *
 * @author wspride
 */
public class NameHasher extends DefaultHasher {

    private final static int CLASS_HASH_SIZE = 32;

    // Overrides the DefaultHasher getHash()
    public byte[] getHash(Class c){
        // reverse because the beginning of the classpath is less likely to be unique than the name
        return new StringBuilder(c.getName()).reverse().toString().getBytes();
    }

    // Overrides the DefaultHasher getHashSize()
    public int getHashSize(){
        return CLASS_HASH_SIZE;
    }
}
