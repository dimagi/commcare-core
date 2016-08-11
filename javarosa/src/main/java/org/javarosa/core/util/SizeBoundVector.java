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

    SizeBoundVector(int sizeLimit) {
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
        // TODO: remove this
    }

    public void addBadAudioReference() {
        // TODO: remove this
    }

    public void addBadVideoReference() {
        // TODO: remove this
    }
}
