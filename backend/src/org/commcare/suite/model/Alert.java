package org.commcare.suite.model;

/**
 * Created by Saumya on 7/8/2016.
 */
public class Alert {

    private String xPathCondition;
    private String db;
    private String dbPath;
    private String xPathReference;


    public Alert(String condition, String db, String path, String xPathRef){
        this.xPathCondition = condition;
        this.db = db;
        this.dbPath = path;
        this.xPathReference = xPathRef;
    }

    public String getxPathCondition(){
        return xPathCondition;
    }

    public String getDb(){
        return db;
    }

    public String getDbPath(){
        return dbPath;
    }

    public String getXPathRef(){
        return xPathReference;
    }

}
