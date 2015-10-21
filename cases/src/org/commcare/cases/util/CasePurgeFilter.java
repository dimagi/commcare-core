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
     * Owned by the user or a group
     */
    private static final int STATUS_OWNED = 1;

    /**
     * Should be included in the set of cases seen by the user
     */
    private static final int STATUS_RELEVANT = 2;

    /**
     * Isn't precluded from being included in a sync for any reason
     */
    private static final int STATUS_AVAILABLE = 4;

    /**
     * Should remain on the phone.
     */
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

    private void setIdsToRemoveWithNewExtensions(IStorageUtilityIndexed<Case> caseStorage, Vector<String> owners) {
        //Create a DAG. The Index will be the case GUID. The Nodes will be a int array containing
        //[STATUS_FLAGS, storageid], Edges are a string representing the relationship between the
        //nodes, which is one of the Case Index relationships (IE: parent, extension)
        DAG<String, int[], String> graph = new DAG<String, int[], String>();

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
            if (owned) {
                nodeStatus |= STATUS_OWNED;
            }

            if (!c.isClosed()) {
                nodeStatus |= STATUS_OPEN;
            }

            if (owned && !c.isClosed()) {
                nodeStatus |= STATUS_RELEVANT;
            }

            graph.addNode(c.getCaseId(), new int[]{nodeStatus, c.getID()});

            for (CaseIndex index : indexHolder) {
                graph.setEdge(c.getCaseId(), index.getTarget(), index.getRelationship());
            }
            indexHolder.removeAllElements();
        }

        propogateRelevance(graph);
        propogateAvailabile(graph);
        propogateLive(graph);

        //Ok, so now just go through all nodes and signal that we need to remove anything
        //that isn't live!
        for (Enumeration iterator = graph.getNodes(); iterator.hasMoreElements(); ) {
            int[] node = (int[])iterator.nextElement();
            if (!caseStatusIs(node[0], STATUS_ALIVE)) {
                idsToRemove.addElement(new Integer(node[1]));
            }
        }
    }

    private void propogateRelevance(DAG<String, int[], String> g) {
        propogateMarkToDAG(g, true, STATUS_RELEVANT, STATUS_RELEVANT);
        propogateMarkToDAG(g, false, STATUS_RELEVANT, STATUS_RELEVANT, CaseIndex.RELATIONSHIP_EXTENSION);
    }

    private void propogateAvailabile(DAG<String, int[], String> g) {
        for (Enumeration e = g.getIndices(); e.hasMoreElements(); ) {
            String index = (String)e.nextElement();
            int[] node = g.getNode(index);
            if (caseStatusIs(node[0], STATUS_OPEN | STATUS_RELEVANT) &&
                    !hasOutgoingExtension(g, index)) {
                node[0] |= STATUS_AVAILABLE;
            }
        }
        propogateMarkToDAG(g, false, STATUS_AVAILABLE, STATUS_AVAILABLE, CaseIndex.RELATIONSHIP_EXTENSION);
    }

    private boolean hasOutgoingExtension(DAG<String, int[], String> g, String index) {
        for (Edge<String, String> edge : g.getChildren(index)) {
            if (edge.e.equals(CaseIndex.RELATIONSHIP_EXTENSION)) {
                return true;
            }
        }
        return false;
    }

    private void propogateLive(DAG<String, int[], String> g) {
        for (Enumeration e = g.getIndices(); e.hasMoreElements(); ) {
            String index = (String)e.nextElement();
            int[] node = g.getNode(index);
            if (caseStatusIs(node[0], STATUS_OWNED | STATUS_RELEVANT | STATUS_AVAILABLE)) {
                node[0] |= STATUS_ALIVE;
            }
        }

        propogateMarkToDAG(g, true, STATUS_ALIVE, STATUS_ALIVE);
        propogateMarkToDAG(g, false, STATUS_ALIVE, STATUS_ALIVE, CaseIndex.RELATIONSHIP_EXTENSION);
    }

    private void propogateMarkToDAG(DAG<String, int[], String> g, boolean direction, int mask, int mark) {
        propogateMarkToDAG(g, direction, mask, mark, null);
    }

    /**
     * Propogates the provided mark in a chain from all nodes which meet the mask to all of their
     * neighboring nodes, as long as those nodes meet the relationship provided. If the relationship
     * is null, only the mask is checked.
     *
     * @param dag
     * @param walkFromSourceToSink If true, start at sources (nodes with only outgoing edges), and walk edges
     *                             from parent to child. If false, start at sinks (nodes with only incoming edges)
     *                             and walk from child to parent
     * @param maskCondition        A mask for what nodes meet the criteria of being marked in the walk.
     * @param markToApply          A new binary flag (or set of flags) to apply to all nodes meeting the criteria
     * @param relationship         If non-null, an additional criteria for whether a node should be marked.
     *                             A node will only be marked if the edge walked to put it on the stack
     *                             meets this criteria.
     */
    private void propogateMarkToDAG(DAG<String, int[], String> dag, boolean walkFromSourceToSink,
                                    int maskCondition, int markToApply, String relationship) {
        Stack<String> toProcess = walkFromSourceToSink ? dag.getSources() : dag.getSinks();
        while (!toProcess.isEmpty()) {
            //current node
            String index = toProcess.pop();
            int[] node = dag.getNode(index);

            Vector<Edge<String, String>> edgeSet = walkFromSourceToSink ? dag.getChildren(index) :
                    dag.getParents(index);

            for (Edge<String, String> edge : edgeSet) {
                if (caseStatusIs(node[0], maskCondition) && (relationship == null || edge.e.equals(relationship))) {
                    dag.getNode(edge.i)[0] |= markToApply;
                }
                toProcess.addElement(edge.i);
            }
        }
    }

    private boolean caseStatusIs(int status, int flag) {
        return (status & flag) == flag;
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
