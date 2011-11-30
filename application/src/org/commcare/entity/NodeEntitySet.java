/**
 * 
 */
package org.commcare.entity;

import java.util.Vector;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.Iterator;
import org.javarosa.entity.model.EntitySet;
import org.javarosa.model.xform.XPathReference;

/**
 * @author ctsims
 *
 */
public class NodeEntitySet implements EntitySet<TreeReference> {
	
	private Vector<TreeReference> set;
	
	public NodeEntitySet(String path, EvaluationContext context) {
		set = context.expandReference(XPathReference.getPathExpr(path).getReference(true));
	}

	public int getCount() {
		return set.size();
	}

	public TreeReference get(int index) {
		//TODO: How to index this properly? Not absolute...
		return set.elementAt(index);
	}

	public Iterator<TreeReference> iterate() {
		return new VectorIterator<TreeReference>(set);
	}

	public int getId(TreeReference e) {
		for(int i = 0; i < set.size() ; ++i) {
			if(e == set.elementAt(i)) {
				return i;
			}
		}
		return -1;
	}

}
