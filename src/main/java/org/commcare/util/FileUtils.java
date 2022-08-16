package org.commcare.util;

import org.commcare.cases.util.StringUtils;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.util.ArrayUtilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * Common file operations
 */
public class FileUtils {

    /**
     * Makes a copy of file represented by inputStream to dstFile
     *
     * @param inputStream inputStream for File that needs to be copied
     * @param dstFile     destination File where we need to copy the inputStream
     */
    public static void copyFile(InputStream inputStream, File dstFile) throws IOException {
        if (inputStream == null) {
            return;
        }
        try (OutputStream outputStream = new FileOutputStream(dstFile)) {
            StreamsUtil.writeFromInputToOutputUnmanaged(inputStream, outputStream);
        } finally {
            inputStream.close();
        }
    }

    /**
     * Tries to get content type of a file
     *
     * @param file File we need to know the content type for
     * @return content type for the given file or null
     */
    @Nullable
    public static String getContentType(File file) {
        try {
            InputStream fis = new FileInputStream(file);
            String contentType = URLConnection.guessContentTypeFromStream(fis);
            if (!StringUtils.isEmpty(contentType)) {
                return contentType;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return URLConnection.guessContentTypeFromName(file.getName());
    }

    /**
     * Extracts extension of a file from it's name
     * @param file name or path for the file
     * @return extension of given file
     */
    public static String getExtension(String file) {
        if (file != null && file.contains(".")) {
            return ArrayUtilities.last(file.split("\\."));
        }
        return "";
    }
}
