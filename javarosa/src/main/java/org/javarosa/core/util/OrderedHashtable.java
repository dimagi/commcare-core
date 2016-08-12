package org.javarosa.core.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class OrderedHashtable<K, V> extends Hashtable<K, V> {
    private final Vector<K> orderedKeys;

    public OrderedHashtable() {
        super();
        orderedKeys = new Vector<>();
    }

    public OrderedHashtable(int initialCapacity) {
        super(initialCapacity);
        orderedKeys = new Vector<>(initialCapacity);
    }

    @Override
    public void clear() {
        orderedKeys.removeAllElements();
        super.clear();
    }

    public V elementAt(int index) {
        return get(keyAt(index));
    }

    @Override
    public Enumeration<V> elements() {
        Vector<V> elements = new Vector<>();
        for (int i = 0; i < size(); i++) {
            elements.addElement(elementAt(i));
        }
        return elements.elements();
    }

    public int indexOfKey(K key) {
        return orderedKeys.indexOf(key);
    }

    public Object keyAt(int index) {
        return orderedKeys.elementAt(index);
    }

    @Override
    public Enumeration<K> keys() {
        return orderedKeys.elements();
    }

    @Override
    public V put(K key, V value) {
        if (key == null) {
            throw new NullPointerException();
        }

        V v = super.put(key, value);
        //Check to see whether this grew after the put.
        //(We can't check for much else because this call
        //can be repeated inside of the put).
        if (super.size() > orderedKeys.size()) {
            orderedKeys.addElement(key);
        }
        return v;
    }

    @Override
    public V remove(Object key) {
        orderedKeys.removeElement(key);
        return super.remove(key);
    }

    public void removeAt(int i) {
        remove(keyAt(i));
        orderedKeys.removeElementAt(i);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (Enumeration e = keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            sb.append(key.toString());
            sb.append(" => ");
            sb.append(get(key).toString());
            if (e.hasMoreElements())
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}