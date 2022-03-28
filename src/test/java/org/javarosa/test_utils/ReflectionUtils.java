package org.javarosa.test_utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * ReflectionUtils is a collection of reflection-based utility methods for use in
 * unit testing scenarios.
 *
 * Code adapter from org.springframework.test.util.ReflectionTestUtils
 */
public class ReflectionUtils {
    /**
     * Get the value of an objects field that is not accessible via a normal getter.
     */
    public static Object getField(Object targetObject, String name)
            throws IllegalAccessException {
        Class<? extends Object> targetClass = targetObject.getClass();

        Field field = ReflectionUtils.findField(targetClass, name);
        if (field == null) {
            throw new IllegalArgumentException(String.format(
                    "Could not find field '%s' on %s or target class [%s]",
                    name, String.format("target object [%s]", targetObject), targetClass));
        }

        ReflectionUtils.makeAccessible(field);
        return field.get(targetObject);
    }

    public static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) ||
                !Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
                Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    public static Field findField(Class<?> clazz, String name) {
        Class<?> searchType = clazz;
        while (Object.class != searchType && searchType != null) {
            Field[] fields = getDeclaredFields(searchType);
            for (Field field : fields) {
                if (name.equals(field.getName())) {
                    return field;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    private static Field[] getDeclaredFields(Class<?> clazz) {
        try {
            return clazz.getDeclaredFields();
        }
        catch (Throwable ex) {
            throw new IllegalStateException("Failed to introspect Class [" + clazz.getName() +
                    "] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
        }
    }
}
