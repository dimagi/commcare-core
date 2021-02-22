package org.commcare.suite.model;

import javax.annotation.Nullable;

/**
 * Created by wpride1 on 4/24/15.
 *
 * Represents an evaluated DisplayUnit
 */
public class DisplayData {

    final String name;
    @Nullable
    final String imageURI;
    @Nullable
    final String audioURI;
    @Nullable
    final String textForBadge;
    @Nullable
    final String hintText;

    public DisplayData(String name, String imageURI, String audioURI, String badgeText, String hintText) {
        this.name = name;
        this.imageURI = imageURI;
        this.audioURI = audioURI;
        this.textForBadge = badgeText;
        this.hintText = hintText;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getImageURI() {
        return imageURI;
    }

    @Nullable
    public String getAudioURI() {
        return audioURI;
    }

    @Nullable
    public String getTextForBadge() {
        return textForBadge;
    }

    @Nullable
    public String getHintText() {
        return hintText;
    }
}
