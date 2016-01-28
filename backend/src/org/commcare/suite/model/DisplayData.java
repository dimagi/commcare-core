package org.commcare.suite.model;

/**
 * Created by wpride1 on 4/24/15.
 *
 * Represents an evaluated DisplayUnit
 */
public class DisplayData {

    String name;
    String imageURI;
    String audioURI;

    public DisplayData(String name, String imageURI, String audioURI) {
        this.name = name;
        this.imageURI = imageURI;
        this.audioURI = audioURI;
    }

    public String getName() {
        return name;
    }

    public String getImageURI() {
        return imageURI;
    }

    public String getAudioURI() {
        return audioURI;
    }
}
