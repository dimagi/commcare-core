package org.commcare.util;

import org.javarosa.core.io.StreamsUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    /**
     * Makes a copy of file represented by inputStream to dstFile
     *
     * @param inputStream inputStream for File that needs to be copied
     * @param dstFile     destination File where we need to copy the inputStream
     */
    public static void copyFile(InputStream inputStream, File dstFile) throws IOException {
        if (inputStream == null) return;
        try (OutputStream outputStream = new FileOutputStream(dstFile)) {
            StreamsUtil.writeFromInputToOutputUnmanaged(inputStream, outputStream);
        } finally {
            inputStream.close();
        }
    }
}
