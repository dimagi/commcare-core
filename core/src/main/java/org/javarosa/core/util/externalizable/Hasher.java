package org.javarosa.core.util.externalizable;

/**
 * Abstract hashing class defining the basic outline of performing a hash. Hasher
 * implementations must override getHash and getHashSize
 *
 * @author ctsims
 * @author wspride
 */
public abstract class Hasher {

    public byte[] getClassHashValue(Class type){
        byte[] returnHash = new byte[this.getHashSize()];
        byte[] computedHash = getHash(type); //add support for a salt, in case of collision?

        for(int i=0; i< returnHash.length && i<computedHash.length; i++){
            returnHash[i] = computedHash[i];
        }

        return returnHash;
    }
    public abstract int getHashSize();
    public abstract byte[] getHash(Class c);
}
