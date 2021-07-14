package org.commcare.core.graph.util;

/**
 * @author $|-|!˅@M
 */
public class StringWidthUtil {
    private static AbsStringExtension stringExtension;

    public static void addExtension(AbsStringExtension extension) {
        stringExtension = extension;
    }

    /**
     * Returns the width of string if possible otherwise -1
     */
    public static int getStringWidth(String text) {
        if (stringExtension == null) {
            return -1;
        }
        return stringExtension.getWidth(text);
    }
}
