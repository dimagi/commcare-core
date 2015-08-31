/**
 *
 */
package org.javarosa.core.services.locale;

import org.javarosa.core.util.Map;
import org.javarosa.core.util.OrderedHashtable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author ctsims
 */
public class LocalizationUtils {
    /**
     * @param is A path to a resource file provided in the current environment
     * @return a dictionary of key/value locale pairs from a file in the resource directory
     * @throws IOException
     */
    public static Map<String, String> parseLocaleInput(InputStream is) throws IOException {
        // TODO: This might very well fail. Best way to handle?
        Map<String, String> locale = new Map<String, String>();
        int chunk = 100;
        InputStreamReader isr;
        isr = new InputStreamReader(is, "UTF-8");
        boolean done = false;
        char[] cbuf = new char[chunk];
        int offset = 0;
        int curline = 0;

        String line = "";
        while (!done) {
            int read = isr.read(cbuf, offset, chunk - offset);
            if (read == -1) {
                done = true;
                if (line != "") {
                    parseAndAdd(locale, line, curline);
                }
                break;
            }
            String stringchunk = String.valueOf(cbuf, offset, read);

            int index = 0;

            while (index != -1) {
                int nindex = stringchunk.indexOf('\n', index);
                //UTF-8 often doesn't encode with newline, but with CR, so if we
                //didn't find one, we'll try that
                if (nindex == -1) {
                    nindex = stringchunk.indexOf('\r', index);
                }
                if (nindex == -1) {
                    line += stringchunk.substring(index);
                    break;
                } else {
                    line += stringchunk.substring(index, nindex);
                    //Newline. process our string and start the next one.
                    curline++;
                    parseAndAdd(locale, line, curline);
                    line = "";
                }
                index = nindex + 1;
            }
        }
        return locale;
    }

    public static void parseAndAdd(OrderedHashtable locale, String line, int curline) {

        //trim whitespace.
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

        if (line.indexOf('=') == -1) {
            // TODO: Invalid line. Empty lines are fine, especially with comments,
            // but it might be hard to get all of those.
            if (line.trim().equals("")) {
                //Empty Line
            } else {
                System.out.println("Invalid line (#" + curline + ") read: " + line);
            }
        } else {
            //Check to see if there's anything after the '=' first. Otherwise there
            //might be some big problems.
            if (line.indexOf('=') != line.length() - 1) {
                String value = line.substring(line.indexOf('=') + 1, line.length());
                locale.put(line.substring(0, line.indexOf('=')), parseValue(value));
            } else {
                System.out.println("Invalid line (#" + curline + ") read: '" + line + "'. No value follows the '='.");
            }
        }
    }

        /*
         * Helper to replace our markdown encodings with what we want
         */

    public static String parseValue(String value) {
        String ret = LocalizationUtils.replace(value, "\\#", "#");
        ret = LocalizationUtils.replace(ret, "\\n", "\n");
        return ret;
    }

    /*
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

    /*
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
