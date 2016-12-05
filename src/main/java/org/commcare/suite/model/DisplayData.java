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
    final String textForBadge;

    public DisplayData(String name, String imageURI, String audioURI, String badgeText) {
        this.name = name;
        this.imageURI = imageURI;
        this.audioURI = audioURI;
        this.textForBadge = badgeText;
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

    public String getTextForBadge() {
        return textForBadge;
    }
}
