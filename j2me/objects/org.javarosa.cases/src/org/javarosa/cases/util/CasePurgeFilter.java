/**
 * 
 */
package org.javarosa.cases.util;

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.javarosa.cases.model.Case;
import org.javarosa.cases.model.CaseIndex;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.DAG;
import org.javarosa.core.util.DataUtil;

/**
 * @author ctsims
 *
 */
public class CasePurgeFilter extends EntityFilter<Case> {
	private static final String STATUS_LIVE = "L";
	private static final String STATUS_DEAD = "D";
	private static final String STATUS_CLOSED = "C";
	
	Vector<Integer> idsToRemove = new Vector<Integer>();

	public CasePurgeFilter(IStorageUtilityIndexed<Case> caseStorage) {
		this(caseStorage, null);
	}
		
	/**
	 * Create a filter for purging cases which should no longer be on the phone from
	 * the database. Identifies liveness appropriately based on index dependencies,
	 * along with cases which have no valid owner in the current context. 
	 * 
	 * @param caseStorage The storage which is to be cleaned up.
	 * @param owners A list of IDs for users whose cases should still be on the device.
	 * Any cases which do not have a valid owner will be considered 'closed' when 
	 * determining the purge behavior. Null to not enable
	 * this behavior
	 * 
	 */
	public CasePurgeFilter(IStorageUtilityIndexed<Case> caseStorage, Vector<String> owners) {
		DAG<String, String[]> g = new DAG<String,String[]>();
		
		for(IStorageIterator<Case> i = caseStorage.iterate() ; i.hasMore() ; ) {
			Case c = i.nextRecord();
			boolean owned = true;
			if(owners != null) {
				owned = owners.contains(c.getUserId());
			}
			g.addNode(c.getCaseId(), new String[] {c.isClosed() ? STATUS_CLOSED : (owned ? STATUS_LIVE : STATUS_CLOSED), String.valueOf(c.getID())});
			for(CaseIndex index : c.getIndices()) {
				g.setEdge(c.getCaseId(), index.getTarget());
			}
		}
		
		//We now have our graph. The only nodes which are self-determined are those
		//which are not pointed to by any indexes (the leaves).
		Stack<String> selfDetermined = g.getSources();
		while(!selfDetermined.isEmpty()) {
			
			//current node
			String index = selfDetermined.pop();
			String[] node = g.getNode(index);
			
			
			if(node[0] == STATUS_LIVE) {
				//If it's alive, just remove it from the queue, none of its dependents can
				//be dead
				continue;
			} else {
				//the only option should be closed (dead nodes shouldn't be in the stack), verify.
				if(node[0] != STATUS_CLOSED) {
					throw new RuntimeException("Node " + index + " is somehow dead while in stack");
				}
				//Set the node to the appropriate status
				node[0] = STATUS_DEAD;
				//put it in our purge queue
				idsToRemove.addElement(Integer.valueOf(node[1]));
				
				//Now find all children and add any which are now in charge of their own fate to the stack.
				Vector<String> descendents = g.getChildren(index);
				if(descendents == null) {
					continue;
				}
				for(String dependentIndex : descendents) {
					//This node is now self-determined if all of its parents are dead (it must have at 
					//last one parent, the node current at issue)
					boolean nodeIsSelfDetermined = true;
					for(String parent : g.getParents(dependentIndex)) {
						if(g.getNode(parent)[0] != STATUS_DEAD) {
							nodeIsSelfDetermined = false;
							break;
						}
					}
					if(nodeIsSelfDetermined) {
						selfDetermined.push(dependentIndex);
					}
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.EntityFilter#preFilter(int, java.util.Hashtable)
	 */
	public int preFilter(int id, Hashtable<String, Object> metaData) {
		if(idsToRemove.contains(DataUtil.integer(id))) {
			return PREFILTER_INCLUDE;
		} else {
			return PREFILTER_EXCLUDE;
		}
	}

	public boolean matches(Case e) {
		//We're doing everything with pre-filtering
		return false;
	}

}
