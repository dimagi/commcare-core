package org.commcare.test.utils;

import java.io.File;

/**
 * Created by wpride1 on 8/12/15.
 */
public class SqlTestUtils {
    public static void deleteDatabase(String username){
        String dbName = username+".db";
        File file = new File(dbName);
        if(!file.delete()){
            System.out.println("DB not deleted");
        }
    }
}
