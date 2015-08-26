package org.javarosa.core.model;

/**
 * Annotation to stand in for Android's Table annotation at the Javarosa
 * level which does not support annotations
 *
 * Created by wpride1 on 8/26/15.
 */
public interface TableAnnotation {
    String getStorageKey();
}
