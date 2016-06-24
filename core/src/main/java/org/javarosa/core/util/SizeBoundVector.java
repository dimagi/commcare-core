package org.javarosa.core.util;

import java.util.Vector;

/**
 * Only use for J2ME Compatible Vectors
 *
 * @author ctsims
 */
public class SizeBoundVector<E> extends Vector<E> {

    int limit = -1;
    int additional = 0;

    int badImageReferenceCount = 0;
    int badAudioReferenceCount = 0;
    int badVideoReferenceCount = 0;

    public SizeBoundVector(int sizeLimit) {
        this.limit = sizeLimit;
    }

    @Override
    public synchronized void addElement(E obj) {
        if (this.size() == limit) {
            additional++;
        } else {
            super.addElement(obj);
        }
    }

    public int getAdditional() {
        return additional;
    }

    public void addBadImageReference() {
        badImageReferenceCount++;
    }

    public void addBadAudioReference() {
        badAudioReferenceCount++;
    }

    public void addBadVideoReference() {
        badVideoReferenceCount++;
    }

    @SuppressWarnings("unused")
    public int getBadImageReferenceCount() {
        return badImageReferenceCount;
    }

    @SuppressWarnings("unused")
    public int getBadAudioReferenceCount() {
        return badAudioReferenceCount;
    }

    @SuppressWarnings("unused")
    public int getBadVideoReferenceCount() {
        return badVideoReferenceCount;
    }
}
