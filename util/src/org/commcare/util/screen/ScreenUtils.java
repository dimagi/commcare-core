package org.commcare.util.screen;

/**
 * Generally useful methods on CLI screens.
 *
 * Created by ctsims on 8/20/2015.
 */
public class ScreenUtils {

    public static void addPaddedStringToBuilder(StringBuilder builder, String s, int width) {
        if (s.length() > width) {
            builder.append(s, 0, width);
            return;
        }
        builder.append(s);
        if (s.length() != width) {
            // add whitespace padding
            for (int i = 0; i < width - s.length(); ++i) {
                builder.append(' ');
            }
        }
    }

    public static String pad(String s, int width) {
        StringBuilder builder = new StringBuilder();
        addPaddedStringToBuilder(builder, s, width);
        return builder.toString();
    }

}
