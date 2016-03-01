/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.core.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class OrderedHashtable<K, V> extends Hashtable<K, V> {
    private final Vector<K> orderedKeys;

    public OrderedHashtable() {
        super();
        orderedKeys = new Vector<K>();
    }

    public OrderedHashtable(int initialCapacity) {
        super(initialCapacity);
        orderedKeys = new Vector<K>(initialCapacity);
    }

    public void clear() {
        orderedKeys.removeAllElements();
        super.clear();
    }

    public Object elementAt(int index) {
        return get(keyAt(index));
    }

    public Enumeration elements() {
        Vector elements = new Vector();
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

    public Enumeration keys() {
        return orderedKeys.elements();
    }

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

    public V remove(Object key) {
        orderedKeys.removeElement(key);
        return super.remove(key);
    }

    public void removeAt(int i) {
        remove(keyAt(i));
        orderedKeys.removeElementAt(i);
    }

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