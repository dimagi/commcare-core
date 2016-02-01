package org.commcare.suite.model;

/**
 * Created by wpride1 on 4/27/15.
 *
 * Interface to be implemented by objects that want to be
 * displayed in MenuLists and MenuGrids
 */
public interface MenuDisplayable {

    String getAudioURI();

    String getImageURI();

    String getDisplayText();
}
