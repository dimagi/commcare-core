package org.commcare.suite.model;

/**
 * Created by Saumya on 7/8/2016.
 */
public class Alert {

    private String caseProperty1;
    private String caseProperty2;
    private String caseType;

    public Alert(String c1, String c2, String type){
        caseProperty1 = c1;
        caseProperty2 = c2;
        caseType = type;
    }

    public String getCaseProperty1(){
        return caseProperty1;
    }

    public String getCaseProperty2(){
        return caseProperty2;
    }

    public String getCaseType(){
        return caseType;
    }

}
