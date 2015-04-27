package org.commcare.suite.model.graph;

/**
 * Created by wpride1 on 4/24/15.
 */
public class DisplayData {

    String name;
    String iURI;
    String aURI;

    public DisplayData(String name, String iURI, String aURI){
        this.name = name;
        this.iURI = iURI;
        this.aURI = aURI;
    }

    public String getName(){
        return name;
    }

    public String getImageURI(){
        return iURI;
    }

    public String getAudioURI(){
        return aURI;
    }
}
