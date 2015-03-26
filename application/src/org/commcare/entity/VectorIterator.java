/**
 *
 */
package org.commcare.entity;

import java.util.Vector;

import org.javarosa.core.util.Iterator;

/**
 * @author ctsims
 *
 */
public class VectorIterator<E> implements Iterator<E> {

    Vector<E> set;
    final int[] count = new int[] { 0 };

    public VectorIterator(Vector<E> set) {
        //TODO: Safety: Is this ever going to change?
        this.set = set;
    }

    public int numRecords() {
        return set.size();
    }

    public int peekID() {
        synchronized(count) {
            return count[0];
        }
    }

    public int nextID() {
        synchronized(count) {
            int retVal = count[0];
            count[0] = count[0] + 1;
            return retVal;
        }
    }

    public E nextRecord() {
        synchronized(count) {
            int id = nextID();
            return set.elementAt(id);
        }

    }

    public boolean hasMore() {
        synchronized(count) {
            return peekID() < set.size();
        }
    }

}
