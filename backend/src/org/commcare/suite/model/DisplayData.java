package org.commcare.suite.model;

/**
 * Created by wpride1 on 4/24/15.
 *
 * Represents an evaluated DisplayUnit
 */
public class DisplayData {

    final String name;
    final String imageURI;
    final String audioURI;
    final int numericBadgeCount;

    public DisplayData(String name, String imageURI, String audioURI, int numericBadgeCount) {
        this.name = name;
        this.imageURI = imageURI;
        this.audioURI = audioURI;
        this.numericBadgeCount = numericBadgeCount;
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

    public int getNumericBadgeCount() {
        return numericBadgeCount;
    }
}
