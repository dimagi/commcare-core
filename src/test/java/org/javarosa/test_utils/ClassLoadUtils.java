package org.javarosa.test_utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class ClassLoadUtils {

    /**
     * Filter classes such that they extend the base class and are not abstract
     */
    public static List<Class> classesThatExtend(Set<Class> classes, Class baseClass) {
        List<Class> filteredClasses = new ArrayList<>();
        for (Class cls : classes) {
            if (baseClass.isAssignableFrom(cls)
                    && !Modifier.isAbstract(cls.getModifiers())) {
                filteredClasses.add(cls);
            }
        }
        return filteredClasses;
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages.
     *
     * via http://stackoverflow.com/a/862130
     */
    public static Set<Class> getClasses(String packageName)
            throws ClassNotFoundException, IOException, URISyntaxException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            URI uri = new URI(resource.toString());
            if (uri.getPath() != null) {
                dirs.add(new File(uri.getPath()));
            }
        }
        Set<Class> classes = new HashSet<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }

        return classes;
    }

    /**
     * Recursive method used to find all classes in a given directory and
     * subdirs.
     *
     * via http://stackoverflow.com/a/862130
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     */
    private static List<Class> findClasses(File directory, String packageName)
            throws ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    String fileName = file.getName().substring(0, file.getName().length() - 6);
                    classes.add(Class.forName(packageName + '.' + fileName));
                }
            }
        }
        return classes;
    }
}
