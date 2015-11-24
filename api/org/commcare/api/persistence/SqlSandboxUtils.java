package org.commcare.api.persistence;

import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.File;

/**
 * Methods that mostly are used around the mocks that replicate stuff from
 * other projects.
 *
 * @author ctsims
 * @author wspride
 */
public class SqlSandboxUtils {

    public static UserSqlSandbox getStaticStorage(String username, String path) {
        System.out.println("Get STatic storage path: " + path);
        return new UserSqlSandbox(username, path);
    }

    public static UserSqlSandbox getStaticStorage(String username) {
        return new UserSqlSandbox(username, null);
    }

    public static void deleteDatabaseFolder(){
        deleteDatabaseFolder(null);
    }

    public static void deleteDatabaseFolder(String path){
        File databaseFolder;
        if(path == null) {
            databaseFolder = new File(SqliteIndexedStorageUtility.DATABASE_FOLDER);
        } else{
            databaseFolder = new File(path);
        }
        if (databaseFolder.exists()) {
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
