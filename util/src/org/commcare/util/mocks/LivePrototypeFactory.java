package org.commcare.util.mocks;

import java.util.Hashtable;

import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Hasher;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 *
 * A prototype factory that is configured to keep track of all of the case->hash
 * pairs that it creates in order to use them for deserializaiton in the future.
 * 
 * Will only work reliably if it is used synchronously to hash all values that
 * are read, and should really only be expected to function for 'in memory' storage
 * like mocks.
 * 
 * TODO: unify with Android storage live factory mocker
 * 
 * @author ctsims
 *
 */
public class LivePrototypeFactory extends PrototypeFactory implements Hasher {
    
    Hashtable<String, Class> factoryTable = new Hashtable<String, Class>();
    Hasher mPassThroughHasher;
    
    public LivePrototypeFactory() {
        this(new DefaultHasher());
    }
    
    public LivePrototypeFactory(Hasher hasher) {
        this.mPassThroughHasher = hasher;
    }

    @Override
    protected void lazyInit() {
    }

    @Override
    public void addClass(Class c) {
        byte[] hash = mPassThroughHasher.getClassHashValue(c);
        factoryTable.put(ExtUtil.printBytes(hash), c);
    }

    @Override
    public Class getClass(byte[] hash) {
        String key = ExtUtil.printBytes(hash);
        return factoryTable.get(key);
    }

    @Override
    public Object getInstance(byte[] hash) {
        return PrototypeFactory.getInstance(getClass(hash));
    }

    @Override
    public byte[] getClassHashValue(Class type) {
        byte[] hash = mPassThroughHasher.getClassHashValue(type);
        factoryTable.put(ExtUtil.printBytes(hash), type);
        return hash;
    }

}