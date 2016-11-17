package org.javarosa.core.util;

/**
 * Only use for J2ME Compatible Vectors
 *
 * A SizeBoundVector that enforces that all member items be unique. You must
 * implement the .equals() method of class E
 *
 * @author wspride
 */
public class SizeBoundUniqueVector<E> extends SizeBoundVector<E> {

    public SizeBoundUniqueVector(int sizeLimit) {
        super(sizeLimit);
    }

    @Override
    public synchronized void addElement(E obj) {
        add(obj);
    }

    @Override
    public synchronized boolean add(E obj) {
        if (this.size() == limit) {
            additional++;
            return true;
        } else if (this.contains(obj)) {
            return false;
        } else {
            super.addElement(obj);
            return true;
        }
    }
}