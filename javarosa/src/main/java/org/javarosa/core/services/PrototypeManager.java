package org.javarosa.core.services;

import org.javarosa.core.util.externalizable.CannotCreateObjectException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.util.HashSet;

public class PrototypeManager {
    private static final HashSet<String> prototypes = new HashSet<>();
    private static PrototypeFactory staticDefault;

    public static void registerPrototype(String className) {
        prototypes.add(className);

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

    public static PrototypeFactory getDefault() {
        if (staticDefault == null) {
            rebuild();
        }
        return staticDefault;
    }

    private static void rebuild() {
        if (staticDefault == null) {
            staticDefault = new PrototypeFactory(prototypes);
            return;
        }
        synchronized (staticDefault) {
            staticDefault = new PrototypeFactory(prototypes);
        }
    }

}
