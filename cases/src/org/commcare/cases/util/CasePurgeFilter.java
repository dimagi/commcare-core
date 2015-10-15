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



    private static final int STATUS_OWNED = 1;

    private static final int STATUS_RELEVANT= 2;

    private static final int STATUS_AVAILABLE = 4;

    private static final int STATUS_ALIVE = 8;

    private static final int STATUS_OPEN = 16;

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
        setIdsToRemoveWithNewExtensions(caseStorage, owners);
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
            //Four cases, applied in order. One: closed and starts life dead
            if (c.isClosed()) {
                nodeStatus = STATUS_DEAD;
            } else if(!owned) {
                //two: unowned is dead unless it is an extension
                if(hasExtension(indexHolder)) {
                    nodeStatus = STATUS_ABANDONED;
                } else {
                    nodeStatus = STATUS_DEAD;
                }
            } else {
                //Otherwise we need to see whether this case maintains any extension indices,
                //if so, it's abandoned. Otherwise it's Alive
                boolean abandoned = hasExtension(indexHolder);
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
                idsToRemove.addElement(Integer.valueOf(node[1]));
            }
        }
    }

    private void setIdsToRemoveWithNewExtensions(IStorageUtilityIndexed<Case> caseStorage, Vector<String> owners) {
        //Create a DAG. The Index will be the case GUID. The Nodes will be a int array containing
        //[STATUS_FLAGS, storageid]
        DAG<String, int[], String> g = new DAG<String, int[], String>();

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
            int nodeStatus = 0;
            if(owned) {
                nodeStatus |= STATUS_OWNED;
            }

            if(!c.isClosed()) {
                nodeStatus |= STATUS_OPEN;
            }

            if(owned && !c.isClosed()) {
                nodeStatus |= STATUS_RELEVANT;
            }

            g.addNode(c.getCaseId(), new int[]{nodeStatus, c.getID()});

            for (CaseIndex index : indexHolder) {
                g.setEdge(c.getCaseId(), index.getTarget(), index.getRelationship());
            }
            indexHolder.removeAllElements();
        }

        markRelevant(g);
        markAvailable(g);
        markLive(g);

        //Ok, so now just go through all nodes and signal that we need to remove anything
        //that isn't live!
        for (Enumeration<int[]> iterator = g.getNodes(); iterator.hasMoreElements(); ) {
            int[] node = (int[])iterator.nextElement();
            if (!is(node, STATUS_ALIVE)) {
                idsToRemove.addElement(Integer.valueOf(node[1]));
            }
        }
    }

    private void markRelevant(DAG<String, int[], String> g) {
        walk(g,true, STATUS_RELEVANT, STATUS_RELEVANT);
        walk(g,false, STATUS_RELEVANT, STATUS_RELEVANT, CaseIndex.RELATIONSHIP_EXTENSION);
    }

    private void markAvailable(DAG<String, int[], String> g) {
        for(Enumeration<String> e = g.getIndices(); e.hasMoreElements();) {
            String index = e.nextElement();
            int[] node = g.getNode(index);
            if(is(node, STATUS_OPEN | STATUS_RELEVANT) &&
                    !hasOutgoingExtension(g,index)) {
                node[0] |= STATUS_AVAILABLE;
            }
        }
        walk(g,false, STATUS_AVAILABLE, STATUS_AVAILABLE, CaseIndex.RELATIONSHIP_EXTENSION);
    }

    private void markLive(DAG<String, int[], String> g) {
        for(Enumeration<String> e = g.getIndices(); e.hasMoreElements();) {
            String index = e.nextElement();
            int[] node = g.getNode(index);
            if(is(node, STATUS_OWNED | STATUS_RELEVANT | STATUS_AVAILABLE)) {
                node[0] |= STATUS_ALIVE;
            }
        }

        walk(g, true, STATUS_ALIVE, STATUS_ALIVE);
        walk(g, false, STATUS_ALIVE, STATUS_ALIVE, CaseIndex.RELATIONSHIP_EXTENSION);
    }

    private boolean hasOutgoingExtension(DAG<String, int[], String> g, String index) {
        for(Edge<String, String> edge : g.getChildren(index)) {
            if(edge.e.equals(CaseIndex.RELATIONSHIP_EXTENSION)) {
                return true;
            }
        }
        return false;
    }

    private void walk(DAG<String, int[], String> g, boolean direction, int mask, int mark) {
        walk(g, direction, mask, mark, null);
    }

    private void walk(DAG<String, int[], String> g, boolean direction, int mask, int mark, String relationship) {
        Stack<String> toProcess = direction ? g.getSources() : g.getSinks();
        while (!toProcess.isEmpty()) {
            //current node
            String index = toProcess.pop();
            int[] node = g.getNode(index);

            for (Edge<String, String> edge : (direction ? g.getChildren(index) : g.getParents(index))) {
                if (is(node, mask) && (relationship == null || edge.e.equals(relationship))) {
                    g.getNode(edge.i)[0] |= mark;
                }
                toProcess.addElement(edge.i);
            }
        }
    }

    private boolean is(int[] node, int flag) {
        return (node[0] & flag) == flag;
    }


    private boolean hasExtension(Vector<CaseIndex> indexHolder) {
        for (CaseIndex index : indexHolder) {
            if (index.getRelationship().equals(CaseIndex.RELATIONSHIP_EXTENSION)) {
                return true;
            }
        }
        return false;
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
