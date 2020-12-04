package org.javarosa.core.services;

import org.javarosa.core.util.externalizable.CannotCreateObjectException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.util.HashSet;

public class PrototypeManager {
    private static final HashSet<String> globalPrototypes = new HashSet<>();

    private static final ThreadLocal<PrototypeFactory> threadLocalPrototypeFactory = new ThreadLocal<PrototypeFactory>(){
        @Override
        protected PrototypeFactory initialValue()
        {
            return null;
        }
    };

    private static PrototypeFactory globalStaticDefault;

    private static boolean useThreadLocalStrategy = false;

    public static void useThreadLocalStrategy(boolean useThreadLocal) {
        useThreadLocalStrategy = useThreadLocal;
    }

    public static void registerPrototype(String className) {
        globalPrototypes.add(className);

        try {
            PrototypeFactory.getInstance(Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new CannotCreateObjectException(className + ": not found");
        }
        rebuild();
    }

    public static void registerPrototypes(String[] classNames) {
        for (String className : classNames) {
            registerPrototype(className);
        }
    }

    private static PrototypeFactory getCurrentStaticFactory() {
        if(useThreadLocalStrategy) {
            return threadLocalPrototypeFactory.get();
        } else {
            return globalStaticDefault;
        }
    }

    public static PrototypeFactory getDefault() {
        if (getCurrentStaticFactory() == null) {
            rebuild();
        }
        return getCurrentStaticFactory();
    }

    private static void rebuild() {
        PrototypeFactory currentStaticFactory = getCurrentStaticFactory();
        if (currentStaticFactory == null) {
            if(useThreadLocalStrategy) {
                threadLocalPrototypeFactory.set(new PrototypeFactory((HashSet<String>)globalPrototypes.clone()));
            } else {
                globalStaticDefault = new PrototypeFactory(globalPrototypes);
            }
            return;
        }
        synchronized (currentStaticFactory) {
            if(useThreadLocalStrategy) {
                threadLocalPrototypeFactory.set(new PrototypeFactory((HashSet<String>)globalPrototypes.clone()));
            } else {
                globalStaticDefault = new PrototypeFactory(globalPrototypes);
            }
        }
    }

}