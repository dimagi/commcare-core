/**
 * 
 */
package org.javarosa.core.util;

import java.util.Vector;

/**
 * 
 * Only use for J2ME Compatible Vectors 
 * 
 * @author ctsims
 *
 */
public class SizeBoundVector<E> extends Vector<E> {
	
	int limit = -1;
	int additional = 0;
	
	public SizeBoundVector(int sizeLimit) {
		this.limit = sizeLimit;
	}

	/* (non-Javadoc)
	 * @see java.util.Vector#addElement(java.lang.Object)
	 */
	public synchronized void addElement(E obj) {
		if(this.size() == limit) {
			additional++;
			return;
		} else {
			super.addElement(obj);
		}
	}
	
	public int getAdditional() {
		return additional;
	}

}
