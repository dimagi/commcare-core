package org.commcare.api.persistence;

import java.io.File;

/**
 * Methods that mostly are used around the mocks that replicate stuff from
 * other projects.
 *
 * @author ctsims
 * @author wspride
 */
public class SqlSandboxUtils {

    public static void deleteDatabaseFolder(String path) {
        File databaseFolder = new File(path);
        if (databaseFolder.exists()) {
            deleteFolder(databaseFolder);
        }
    }

    public static boolean databaseFolderExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
}
