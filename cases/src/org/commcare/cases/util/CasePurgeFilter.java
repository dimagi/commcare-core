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

    private final Vector<Integer> idsToRemove = new Vector<Integer>();

    // The index is a string containing the case GUID. The Nodes will be a int array containing
    // [STATUS_FLAGS, storageid]. Edges are a string representing the relationship between the
    // nodes, which is one of the Case Index relationships (IE: parent, extension)
    private DAG<String, int[], String> internalCaseDAG;

    // Flag that gets checked by DataPullTask, in order to report to device logs
    private boolean invalidEdgesWereRemoved;
    // List of case ids for cases that were indexed and expected to be on the phone, but were
    // actually not present
    private Vector<String> missingCases = new Vector<String>();
    // List of case ids for cases that were deleted off of the device as a result missing cases
    private Vector<String> casesRemovedDueToMissingCases = new Vector<String>();

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
        internalCaseDAG = new DAG<String, int[], String>();

        Vector<CaseIndex> indexHolder = new Vector<CaseIndex>();

        // Pass 1: Create a DAG which contains all of the cases on the phone as nodes, and has a
        // directed edge for each index (from the 'child' case pointing to the 'parent' case) with
        // the appropriate relationship tagged
        for (IStorageIterator<Case> i = caseStorage.iterate(); i.hasMore(); ) {
            Case c = i.nextRecord();
            boolean owned = true;
            if (owners != null) {
                owned = owners.contains(c.getUserId());
            }

            // In order to deal with multiple indices pointing to the same case with different
            // relationships, we'll need to traverse once to eliminate any ambiguity
            // TODO: How do we speed this up? Do we need to?
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

            internalCaseDAG.addNode(c.getCaseId(), new int[]{nodeStatus, c.getID()});

            for (CaseIndex index : indexHolder) {
                internalCaseDAG.setEdge(c.getCaseId(), index.getTarget(), index.getRelationship());
            }
            indexHolder.removeAllElements();
        }


        // It is important that actual edge removal be done after the call to getInvalidEdges() is
        // complete, to prevent a ConcurrentModificationException
        Vector<String[]> edgesToRemove = getInvalidEdges();
        for (String[] edge : edgesToRemove) {
            internalCaseDAG.removeEdge(edge[0], edge[1]);
        }

        propagateRelevance(internalCaseDAG);
        propagateAvailabile(internalCaseDAG);
        propagateLive(internalCaseDAG);

        // Ok, so now just go through all nodes and signal that we need to remove anything
        // that isn't live!
        for (Enumeration iterator = internalCaseDAG.getNodes(); iterator.hasMoreElements(); ) {
            int[] node = (int[])iterator.nextElement();
            if (!caseStatusIs(node[0], STATUS_ALIVE)) {
                idsToRemove.addElement(new Integer(node[1]));
            }
        }
    }

    private static void propagateRelevance(DAG<String, int[], String> g) {
        propagateMarkToDAG(g, true, STATUS_RELEVANT, STATUS_RELEVANT);
        propagateMarkToDAG(g, false, STATUS_RELEVANT, STATUS_RELEVANT, CaseIndex.RELATIONSHIP_EXTENSION, false);
    }

    private static void propagateAvailabile(DAG<String, int[], String> g) {
        for (Enumeration e = g.getIndices(); e.hasMoreElements(); ) {
            String index = (String)e.nextElement();
            int[] node = g.getNode(index);
            if (caseStatusIs(node[0], STATUS_OPEN | STATUS_RELEVANT) &&
                    !hasOutgoingExtension(g, index)) {
                node[0] |= STATUS_AVAILABLE;
            }
        }
        propagateMarkToDAG(g, false, STATUS_AVAILABLE, STATUS_AVAILABLE, CaseIndex.RELATIONSHIP_EXTENSION, true);
    }

    private static boolean hasOutgoingExtension(DAG<String, int[], String> g, String index) {
        for (Edge<String, String> edge : g.getChildren(index)) {
            if (edge.e.equals(CaseIndex.RELATIONSHIP_EXTENSION)) {
                return true;
            }
        }
        return false;
    }

    private static void propagateLive(DAG<String, int[], String> g) {
        for (Enumeration e = g.getIndices(); e.hasMoreElements(); ) {
            String index = (String)e.nextElement();
            int[] node = g.getNode(index);
            if (caseStatusIs(node[0], STATUS_OWNED | STATUS_RELEVANT | STATUS_AVAILABLE)) {
                node[0] |= STATUS_ALIVE;
            }
        }

        propagateMarkToDAG(g, true, STATUS_ALIVE, STATUS_ALIVE);
        propagateMarkToDAG(g, false, STATUS_ALIVE, STATUS_ALIVE, CaseIndex.RELATIONSHIP_EXTENSION, true);
    }

    private static void propagateMarkToDAG(DAG<String, int[], String> g, boolean direction, int mask, int mark) {
        propagateMarkToDAG(g, direction, mask, mark, null, false);
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
    private static void propagateMarkToDAG(DAG<String, int[], String> dag, boolean walkFromSourceToSink,
                                    int maskCondition, int markToApply, String relationship,
                                    boolean requireOpenDestination) {
        Stack<String> toProcess = walkFromSourceToSink ? dag.getSources() : dag.getSinks();
        while (!toProcess.isEmpty()) {
            // current node
            String index = toProcess.pop();
            int[] node = dag.getNode(index);

            Vector<Edge<String, String>> edgeSet = walkFromSourceToSink ? dag.getChildren(index) :
                    dag.getParents(index);

            for (Edge<String, String> edge : edgeSet) {
                if (caseStatusIs(node[0], maskCondition) && (relationship == null || edge.e.equals(relationship))) {
                    if(!requireOpenDestination || caseStatusIs(dag.getNode(edge.i)[0], STATUS_OPEN)) {
                        dag.getNode(edge.i)[0] |= markToApply;
                    }
                }
                toProcess.addElement(edge.i);
            }
        }
    }

    /**
     * Traverse the graph to accumulate a list of any edges to empty nodes (which are created when
     * a child makes a placeholder index to a parent, but then the parent does not actually exist
     * on the phone for some reason).
     *
     * Then remove any nodes that are made invalid by that parent node not existing, which further
     * accumulates the list of invalid edges
     *
     * @return Whether or not this method invocation removed any invalid edges from the DAG
     */
    private Vector<String[]> getInvalidEdges() {
        Hashtable<String, Vector<Edge<String, String>>> allEdges = internalCaseDAG.getEdges();
        Vector<String> childOfNonexistentParent = new Vector<String>();
        Vector<String[]> edgesToRemove = new Vector<String[]>();
        Enumeration edgeOriginIndices = allEdges.keys();
        while (edgeOriginIndices.hasMoreElements()) {
            String originIndex = (String)edgeOriginIndices.nextElement();
            Vector<Edge<String, String>> edgeListForOrigin = allEdges.get(originIndex);
            for (Edge<String, String> edge : edgeListForOrigin) {
                String targetIndex = edge.i;
                if (internalCaseDAG.getNode(targetIndex) == null) {
                    missingCases.addElement(targetIndex);
                    edgesToRemove.addElement(new String[]{originIndex, targetIndex});
                    childOfNonexistentParent.addElement(originIndex);
                }
            }
        }

        // Any case node with a nonexistent parent should be removed from the graph, as should
        // all of its descendants, and any edges to or from it
        for (String index : childOfNonexistentParent) {
            removeNodeAndPropagate(index, edgesToRemove);
        }

        invalidEdgesWereRemoved = edgesToRemove.size() > 0;
        return edgesToRemove;
    }

    /**
     * Remove from the graph the node at this index, and remove all nodes that are made
     * invalid by the non-existence of that node (i.e. all of its child and extension cases).
     * Also accumulate a list of edges that need to be removed as a result of this method call.
     * Actual edge removal should then be done all at once by the caller, to avoid
     * ConcurrentModificationException
     */
    private void removeNodeAndPropagate(String indexOfRemovedNode,
                                        Vector<String[]> accumulatedEdgesToRemove) {
        // Wording is confusing here -- because all edges in this graph are from a child case to
        // a parent case, calling getParents() for a node returns all of its child/extension cases
        Vector<Edge<String, String>> childCases = internalCaseDAG.getParents(indexOfRemovedNode);
        for (Edge<String, String> child : childCases) {
            // Want to remove the edge from child case to parent case
            accumulatedEdgesToRemove.addElement(new String[]{child.i, indexOfRemovedNode});

            // Recurse on child case
            removeNodeAndPropagate(child.i, accumulatedEdgesToRemove);
        }

        // Also want to remove any outgoing edges to other parents (but do NOT delete those
        // parents because they are still valid)
        Vector<Edge<String, String>> parentCases = internalCaseDAG.getChildren(indexOfRemovedNode);
        for (Edge<String, String> parent : parentCases) {
            accumulatedEdgesToRemove.addElement(new String[]{indexOfRemovedNode, parent.i});
        }

        // Once all edges to/from this node have been removed, delete the node itself from the
        // DAG, and add it to the list of cases to be purged
        int storageIdOfRemovedNode = internalCaseDAG.removeNode(indexOfRemovedNode)[1];
        idsToRemove.addElement(new Integer(storageIdOfRemovedNode));
        casesRemovedDueToMissingCases.addElement(indexOfRemovedNode);
    }

    // For use in tests
    public DAG<String, int[], String> getInternalCaseGraph() {
        return internalCaseDAG;
    }

    /**
     * When the underlying case DAG for this case purge filter was created, were there any invalid
     * edges (i.e. edges to non-existent nodes) that had to be removed?
     */
    public boolean invalidEdgesWereRemoved() {
        return this.invalidEdgesWereRemoved;
    }

    public String getMissingCasesString() {
        return flattenVectorOfStrings(this.missingCases);
    }

    public String getRemovedCasesString() {
        return flattenVectorOfStrings(this.casesRemovedDueToMissingCases);
    }

    private static String flattenVectorOfStrings(Vector<String> v) {
        StringBuilder builder = new StringBuilder();
        for (String caseId : v) {
            builder.append(caseId + " ");
        }
        return builder.toString();
    }

    private static boolean caseStatusIs(int status, int flag) {
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
