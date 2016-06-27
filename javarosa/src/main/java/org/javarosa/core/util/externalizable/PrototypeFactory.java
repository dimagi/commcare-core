package org.javarosa.core.util.externalizable;

import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.util.PrefixTree;

import java.util.Date;
import java.util.Vector;

/**
 * ProtoType factory for serializing and deserializing persisted classes using
 * their hash codes. To use a non-default hasher, use one of the overriding constructors
 * or call setStaticHasher().
 */
public class PrototypeFactory {

    private static Hasher mStaticHasher;

    private Vector<Class> classes;
    private Vector<byte[]> hashes;

    //lazy evaluation
    private PrefixTree classNames;
    protected boolean initialized;

    public PrototypeFactory() {
        this(null, null);
    }

    public PrototypeFactory(PrefixTree classNames) {
        this.classNames = classNames;
        initialized = false;
        if (mStaticHasher == null) {
            mStaticHasher = new ClassNameHasher();
        }
    }

    public PrototypeFactory(Hasher hasher) {
        this(hasher, null);
    }

    public PrototypeFactory(Hasher hasher, PrefixTree classNames) {
        this.classNames = classNames;
        initialized = false;
        if (mStaticHasher == null) {
            if (hasher == null) {
                mStaticHasher = new ClassNameHasher();
            } else {
                PrototypeFactory.setStaticHasher(hasher);
            }
        }
    }

    protected void lazyInit() {
        initialized = true;

        classes = new Vector<>();
        hashes = new Vector<>();

        addDefaultClasses();
        addMigratedClasses();

        if (classNames != null) {
            for (String name : classNames.getStrings()) {
                try {
                    addClass(Class.forName(name));
                } catch (ClassNotFoundException cnfe) {
                    throw new CannotCreateObjectException(name + ": not found");
                }
            }
            classNames = null;
        }
    }

    /**
     * Override to provide migration logic; needed if classes are renamed,
     * since classes in prototype factory are indexed by classname
     */
    protected void addMigratedClasses() {
    }

    private void addDefaultClasses() {
        Class[] baseTypes = {
                Object.class,
                Integer.class,
                Long.class,
                Short.class,
                Byte.class,
                Character.class,
                Boolean.class,
                Float.class,
                Double.class,
                String.class,
                Date.class,
                UncastData.class
        };

        for (Class baseType : baseTypes) {
            addClass(baseType);
        }
    }

    public void addClass(Class c) {
        if (!initialized) {
            lazyInit();
        }

        byte[] hash = getClassHash(c);

        if (compareHash(hash, PrototypeFactory.getWrapperTag())) {
            throw new Error("Hash collision! " + c.getName() + " and reserved wrapper tag");
        }

        Class d = getClass(hash);
        if (d != null && d != c) {
            throw new Error("Hash collision! " + c.getName() + " and " + d.getName());
        }
        storeHash(c, hash);
    }

    public Class getClass(byte[] hash) {
        if (!initialized) {
            lazyInit();
        }

        for (int i = 0; i < classes.size(); i++) {
            if (compareHash(hash, hashes.elementAt(i))) {
                return classes.elementAt(i);
            }
        }

        return null;
    }

    public Object getInstance(byte[] hash) {
        return getInstance(getClass(hash));
    }

    public static Object getInstance(Class c) {
        try {
            return c.newInstance();
        } catch (IllegalAccessException iae) {
            throw new CannotCreateObjectException(c.getName() + ": not accessible or no empty constructor");
        } catch (InstantiationException e) {
            throw new CannotCreateObjectException(c.getName() + ": not instantiable");
        }
    }

    public static byte[] getClassHash(Class type) {
        return mStaticHasher.getClassHashValue(type);
    }

    public static boolean compareHash(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }

    public static void setStaticHasher(Hasher staticHasher) {
        mStaticHasher = staticHasher;
    }

    public static int getClassHashSize(){
        return mStaticHasher.getHashSize();
    }

    protected void storeHash(Class c, byte[] hash){
        classes.addElement(c);
        hashes.addElement(hash);
    }

    public static byte[] getWrapperTag(){
        byte[] bytes = new byte[getClassHashSize()];
        for(int i=0; i< bytes.length; i++){
            bytes[i] = (byte)0xff;
        }
        return bytes;
    }
}
