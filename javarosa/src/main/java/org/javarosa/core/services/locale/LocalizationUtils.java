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
        while ((i = LocalizationUtils.lastIndexOf(line.substring(0, dec), "#")) != -1) {
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
        String ret = LocalizationUtils.replace(value, "\\#", "#");
        ret = LocalizationUtils.replace(ret, "\\n", "\n");
        return ret;
    }

    /**
     * http://stackoverflow.com/questions/10626606/replace-string-with-string-in-j2me
     */
    private static String replace(String str, String pattern, String replace) {
        int s = 0;
        int e = 0;
        StringBuffer result = new StringBuffer();

        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e + pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }

    /**
     * http://www.experts-exchange.com/Programming/Languages/Java/Q_27604323.html
     */
    private static int lastIndexOf(String str, String search) {
        int i = 0;
        int offset = 0;
        int found = -1;

        while (offset < str.length()) {
            i = str.indexOf(search, offset);
            if (i == -1) break;

            found = i;

            offset = i + 1;
        }
        return found;
    }
}
