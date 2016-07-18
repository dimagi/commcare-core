package org.commcare.suite.model;

/**
 * Created by Saumya on 7/8/2016.
 */
public class Alert {

    private String condition;
    private String caseType;

    public Alert(String type, String exp){
        caseType = type;
        condition = exp;
    }

    public String getCondition(){
        return condition;
    }

    public String getCaseType(){
        return caseType;
    }

}
