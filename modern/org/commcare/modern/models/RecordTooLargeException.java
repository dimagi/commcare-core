package org.commcare.modern.models;

/**
 * Created by wpride1 on 9/23/15.
 */
public class RecordTooLargeException extends RuntimeException {
    public RecordTooLargeException(double size){
        super("You tried to restore some data sized "
                + size + " MB which exceeds" +
                "the maximum allowed size of 1MB. Please reduce the size of this fixture.");
    }
}
