package org.javarosa.core.services.locale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

/**
 * @author ctsims
 */
public class LocalizationUtils {
    /**
     * @param is A path to a resource file provided in the current environment
     * @return a dictionary of key/value locale pairs from a file in the resource directory
     */
    public static Hashtable<String, String> parseLocaleInput(InputStream is) throws IOException {
        Hashtable<String, String> locale = new Hashtable<>();
        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        BufferedReader reader = new BufferedReader(isr);

        String line = reader.readLine();
        int lineCount = 0;
        while (line != null) {
            parseAndAdd(locale, line, lineCount++);
            line = reader.readLine();
        }

        return locale;
    }

    public static void parseAndAdd(Hashtable<String, String> locale, String line, int curline) {
        line = line.trim();

        int i = 0;
        int dec = line.length();

        //clear comments except if they have backslash before them (markdown '#'s)
        while ((i = line.substring(0, dec).lastIndexOf("#")) != -1) {
            if ((i == 0) || !(line.charAt(i - 1) == '\\')) {
                line = line.substring(0, i);
                dec = line.length();
            } else {
                dec = i;
            }
        }

        int equalIndex = line.indexOf('=');
        if (equalIndex == -1) {
            if (!line.trim().equals("")) {
                System.out.println("Invalid line (#" + curline + ") read: " + line);
            }
        } else {
            //Check to see if there's anything after the '=' first. Otherwise there
            //might be some big problems.
            if (equalIndex != line.length() - 1) {
                String value = line.substring(equalIndex + 1, line.length());
                locale.put(line.substring(0, equalIndex), parseValue(value));
            }
        }
    }

    /**
     * Replace markdown encodings
     */
    public static String parseValue(String value) {
        return value.replaceAll("\\\\#", "#").replaceAll("\\\\n", "\n");
    }
}
