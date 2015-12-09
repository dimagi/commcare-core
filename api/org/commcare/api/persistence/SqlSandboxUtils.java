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
    public static UserSqlSandbox getStaticStorage(String username) {
        return new UserSqlSandbox(username);
    }

    public static void deleteDatabaseFolder(){
        File databaseFolder = new File(SqliteIndexedStorageUtility.DATABASE_FOLDER);
        if(databaseFolder.exists()){
            deleteFolder(databaseFolder);
        }
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
}
