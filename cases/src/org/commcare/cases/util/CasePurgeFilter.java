/**
 *
 */
package org.commcare.cases.util;

import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.DAG;
import org.javarosa.core.util.DAG.Edge;
import org.javarosa.core.util.DataUtil;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

/**
 * @author ctsims
 */
public class CasePurgeFilter extends EntityFilter<Case> {

    /**
     * Open and owned by someone on the phone
     */
    private static final String STATUS_LIVE = "L";
    /**
     * Purgable *
     */
    private static final String STATUS_DEAD = "D";
    /**
     * Closed or not owned (NOTE: No longer used) *
     */
    private static final String STATUS_CLOSED = "C";
    /**
     * Not currently alive, and dependent on another case *
     */
    private static final String STATUS_ABANDONED = "A";

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
     * @param owners      A list of IDs for users whose cases should still be on the device.
     *                    Any cases which do not have a valid owner will be considered 'closed' when
     *                    determining the purge behavior. Null to not enable
     *                    this behavior
     */
    public CasePurgeFilter(IStorageUtilityIndexed<Case> caseStorage, Vector<String> owners) {
        setIdsToRemoveNew(caseStorage, owners);
    }

    private void setIdsToRemoveNew(IStorageUtilityIndexed<Case> caseStorage, Vector<String> owners) {
        //Create a DAG. The Index will be the case GUID. The Nodes will be a string array containing
        //[CASE_STATUS, string(storageid)]
        //CASE_STATUS is enumerated as one of STATUS_LIVE, STATUS_DEAD, or STATUS_ABANDONED
        DAG<String, String[], String> g = new DAG<String, String[], String>();

        Vector<CaseIndex> indexHolder = new Vector<CaseIndex>();

        //Pass 1:
        //Create a DAG which contains all of the cases on the phone as nodes, and has a directed
        //edge for each index (from the 'child' case pointing to the 'parent' case) with the
        //appropriate relationship tagged
        for (IStorageIterator<Case> i = caseStorage.iterate(); i.hasMore(); ) {
            Case c = i.nextRecord();
            boolean owned = true;
            if (owners != null) {
                owned = owners.contains(c.getUserId());
            }

            //In order to deal with multiple indices pointing to the same case
            //with different relationships, we'll need to traverse once to eliminate any
            //ambiguity (TODO: How do we speed this up? Do we need to?)
            for (CaseIndex index : c.getIndices()) {
                CaseIndex toReplace = null;
                boolean skip = false;
                for (CaseIndex existing : indexHolder) {
                    if (existing.getTarget().equals(index.getTarget())) {
                        if (existing.getRelationship().equals(CaseIndex.RELATIONSHIP_EXTENSION) && !index.getRelationship().equals(CaseIndex.RELATIONSHIP_EXTENSION)) {
                            toReplace = existing;
                        } else {
                            skip = true;
                        }
                        break;
                    }
                }
                if (toReplace != null) {
                    indexHolder.removeElement(toReplace);
                }
                if (!skip) {
                    indexHolder.addElement(index);
                }
            }
            String nodeStatus;
            //Four cases, applied in order. One and two: closed or unowned it starts life dead
            if (!owned || c.isClosed()) {
                nodeStatus = STATUS_DEAD;
            } else {
                //Otherwise we need to see whether this case maintains any extension indices,
                //if so, it's abandoned. Otherwise it's Alive
                boolean abandoned = false;
                for (CaseIndex index : indexHolder) {
                    if (index.getRelationship().equals(CaseIndex.RELATIONSHIP_EXTENSION)) {
                        abandoned = true;
                        break;
                    }
                }
                if (abandoned) {
                    nodeStatus = STATUS_ABANDONED;
                } else {
                    nodeStatus = STATUS_LIVE;
                }
            }


            g.addNode(c.getCaseId(), new String[]{nodeStatus, String.valueOf(c.getID())});

            for (CaseIndex index : indexHolder) {
                g.setEdge(c.getCaseId(), index.getTarget(), index.getRelationship());
            }
            indexHolder.removeAllElements();
        }

        //Pass 2: Start at Sources (nodes which only produce edges) and walk up
        //the tree, marking relevant nodes as alive
        Stack<String> toProcess = g.getSources();
        while (!toProcess.isEmpty()) {
            //current node
            String index = toProcess.pop();
            String[] node = g.getNode(index);

            //Walk all indexed nodes, adding them to the process stack.
            //and update their liveness if necessary.
            for (Edge<String, String> edge : g.getChildren(index)) {
                if (node[0].equals(STATUS_LIVE)) {
                    g.getNode(edge.i)[0] = STATUS_LIVE;
                }
                //add this to the process stack
                //TODO: We're going to walk stuff redundantly this
                //way. Can we optimize?
                //If parent is already live, does
                //that necessarily denote that its ancestors are alive?
                toProcess.addElement(edge.i);
            }
        }

        //Pass 2: Start at Sinks (nodes which only receive edges) and walk down the
        //graphs, marking necessary nodes as alive
        toProcess = g.getSinks();
        while (!toProcess.isEmpty()) {
            //current node
            String index = toProcess.pop();
            String[] node = g.getNode(index);

            //Walk all indexing nodes.
            for (Edge<String, String> edge : g.getParents(index)) {
                if (g.getNode(edge.i)[0].equals(STATUS_ABANDONED) &&
                        edge.e.equals(CaseIndex.RELATIONSHIP_EXTENSION) &&
                        node[0].equals(STATUS_LIVE)) {

                    g.getNode(edge.i)[0] = STATUS_LIVE;
                }

                //add this to the process stack
                //TODO: We're going to walk stuff redundantly this
                //way. Can we optimize?
                toProcess.addElement(edge.i);
            }
        }

        //Ok, so now just go through all nodes and signal that we need to remove anything
        //that isn't live!
        for (Enumeration iterator = g.getNodes(); iterator.hasMoreElements(); ) {
            String[] node = (String[])iterator.nextElement();
            if (!node[0].equals(STATUS_LIVE)) {
                idsToRemove.addElement(new Integer(node[1]));
            }
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.EntityFilter#preFilter(int, java.util.Hashtable)
     */
    public int preFilter(int id, Hashtable<String, Object> metaData) {
        if (idsToRemove.contains(DataUtil.integer(id))) {
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
