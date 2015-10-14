/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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

    private Vector classes;
    private Vector hashes;

    //lazy evaluation
    private PrefixTree classNames;
    protected boolean initialized;

    public PrototypeFactory() {
        this(null, null);
    }

    public PrototypeFactory(PrefixTree classNames) {
        this.classNames = classNames;
        initialized = false;
        if(mStaticHasher == null){
            mStaticHasher = new ClassNameHasher();
        }
    }


    public PrototypeFactory(Hasher hasher) {
        this(hasher, null);
    }

    public PrototypeFactory(Hasher hasher, PrefixTree classNames) {
        this.classNames = classNames;
        initialized = false;
        if(mStaticHasher == null){
            if(hasher == null) {
                mStaticHasher = new ClassNameHasher();
            } else{
                this.setStaticHasher(hasher);
            }
        }
    }

    protected void lazyInit() {
        initialized = true;

        classes = new Vector();
        hashes = new Vector();

        addDefaultClasses();

        if (classNames != null) {
            Vector vClasses = classNames.getStrings();

            for (int i = 0; i < vClasses.size(); i++) {
                String name = (String)vClasses.elementAt(i);
                try {
                    addClass(Class.forName(name));
                } catch (ClassNotFoundException cnfe) {
                    throw new CannotCreateObjectException(name + ": not found");
                }
            }
            classNames = null;
        }
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

        for (int i = 0; i < baseTypes.length; i++) {
            addClass(baseTypes[i]);
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
            if (compareHash(hash, (byte[])hashes.elementAt(i))) {
                return (Class)classes.elementAt(i);
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
            if (a[i] != b[i])
                return false;
        }

        return true;
    }

    public static void setStaticHasher(Hasher staticHasher) {
        mStaticHasher = staticHasher;
    }

    public static int getClassHashSize(){
        return mStaticHasher.getHashSize();
    }

    public void storeHash(Class c, byte[] hash){
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
