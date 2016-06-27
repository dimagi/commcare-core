package org.javarosa.core.services.locale;

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
        // TODO: This might very well fail. Best way to handle?
        Hashtable<String, String> locale = new Hashtable<>();
        int chunk = 100;
        InputStreamReader isr;
        isr = new InputStreamReader(is, "UTF-8");
        char[] cbuf = new char[chunk];
        int offset = 0;
        int curline = 0;

        String line = "";
        while (true) {
            int read = isr.read(cbuf, offset, chunk - offset);
            if (read == -1) {
                if (!"".equals(line)) {
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
