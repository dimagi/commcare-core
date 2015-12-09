package org.commcare.cases.util.test;

import org.junit.Assert;

import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.core.parse.ParseUtils;
import org.commcare.core.sandbox.SandboxUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.util.DAG;
import org.junit.Test;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

/**
 * Quick test to be able to restore a set of user data
 * and ensure users and groups are properly being included
 * in case purges.
 *
 * @author ctsims
 */
public class CasePurgeRegressions {

    @Test
    public void testSimpleExtensions() throws Exception {
        MockUserDataSandbox sandbox;
        Vector<String> owners;
        sandbox = MockDataUtils.getStaticStorage();

        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().
                getResourceAsStream("case_purge/simple_extension_test.xml"), sandbox);
        owners = SandboxUtils.extractEntityOwners(sandbox);

        CasePurgeFilter purger = new CasePurgeFilter(sandbox.getCaseStorage(), owners);
        int removedCases = sandbox.getCaseStorage().removeAll(purger).size();

        if (removedCases > 0) {
            throw new RuntimeException("Incorrectly removed cases");
        }
    }

    @Test
    public void testValidateCaseGraphBeforePurge_simple() throws Exception {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();
        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().
                getResourceAsStream("case_purge/validate_case_graph_test_simple.xml"), sandbox);
        CasePurgeFilter filter = new CasePurgeFilter(sandbox.getCaseStorage());

        Set<String> nodesExpectedToBeLeft = new HashSet<>();
        nodesExpectedToBeLeft.add("case_one");
        nodesExpectedToBeLeft.add("case_two");

        Set<String[]> edgesExpectedToBeLeft = new HashSet<>();
        edgesExpectedToBeLeft.add(new String[]{"case_two", "case_one"});

        // Check that the edges and nodes still present in the graph are as expected
        DAG<String, int[], String> internalCaseGraph = filter.getInternalCaseGraph();
        checkProperNodesPresent(nodesExpectedToBeLeft, internalCaseGraph);
        checkProperEdgesPresent(edgesExpectedToBeLeft, internalCaseGraph);
    }

    @Test
    public void testValidateCaseGraphBeforePurge_complex() throws Exception {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();
        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().
                getResourceAsStream("case_purge/validate_case_graph_test_complex.xml"), sandbox);
        CasePurgeFilter filter = new CasePurgeFilter(sandbox.getCaseStorage());

        Set<String> nodesExpectedToBeLeft = new HashSet<>();
        nodesExpectedToBeLeft.add("case_one");
        nodesExpectedToBeLeft.add("case_two");

        Set<String[]> edgesExpectedToBeLeft = new HashSet<>();
        edgesExpectedToBeLeft.add(new String[]{"case_two", "case_one"});

        // Check that the edges and nodes still present in the graph are as expected
        DAG<String, int[], String> internalCaseGraph = filter.getInternalCaseGraph();
        checkProperNodesPresent(nodesExpectedToBeLeft, internalCaseGraph);
        checkProperEdgesPresent(edgesExpectedToBeLeft, internalCaseGraph);
    }

    @Test
    public void testValidateCaseGraphBeforePurge_noChange() throws Exception {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();
        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().
                getResourceAsStream("case_purge/validate_case_graph_test_no_change.xml"), sandbox);
        CasePurgeFilter filter = new CasePurgeFilter(sandbox.getCaseStorage());

        Set<String> nodesExpectedToBeLeft = new HashSet<>();
        nodesExpectedToBeLeft.add("case_one");
        nodesExpectedToBeLeft.add("case_two");
        nodesExpectedToBeLeft.add("case_three");
        nodesExpectedToBeLeft.add("case_four");

        Set<String[]> edgesExpectedToBeLeft = new HashSet<>();
        edgesExpectedToBeLeft.add(new String[]{"case_two", "case_one"});

        // Check that the edges and nodes still present in the graph are as expected
        DAG<String, int[], String> internalCaseGraph = filter.getInternalCaseGraph();
        checkProperNodesPresent(nodesExpectedToBeLeft, internalCaseGraph);
        checkProperEdgesPresent(edgesExpectedToBeLeft, internalCaseGraph);
    }

    /**
     * Check that the set of nodes we expect to still be in the case DAG is identical to the
     * nodes actually there
     */
    private static void checkProperNodesPresent(Set<String> nodesExpected,
                                         DAG<String, int[], String> graph) {
        Set<String> nodesActuallyLeft = getSimpleFormNodes(graph.getIndices());
        Assert.assertTrue(nodesExpected.equals(nodesActuallyLeft));
    }

    /**
     * Check that the set of edges we expect to still be in the case DAG is identical to the
     * edges actually there
     */
    private static void checkProperEdgesPresent(Set<String[]> edgesExpected,
                                         DAG<String, int[], String> graph) {
        Set<String[]> edgesActuallyLeft = getSimpleFormEdges(graph.getEdges());
        for (String[] expected : edgesExpected) {
            Assert.assertTrue(checkContainsThisEdge(edgesActuallyLeft, expected));
        }
        for (String[] actual : edgesActuallyLeft) {
            Assert.assertTrue(checkContainsThisEdge(edgesExpected, actual));
        }
    }

    /**
     * Helper method for testing that a set of String[] contains the given String[], based upon
     * content value equality rather than reference equality
     */
    private static boolean checkContainsThisEdge(Set<String[]> setOfEdges, String[] edgeToFind) {
        for (String[] edge : setOfEdges) {
            if (Arrays.equals(edge, edgeToFind)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String[]> getSimpleFormEdges(
            Hashtable<String, Vector<DAG.Edge<String, String>>> edges) {
        Set<String[]> simpleFormEdges = new HashSet<>();
        for (String sourceIndex : edges.keySet()) {
            Vector<DAG.Edge<String, String>> edgesFromSource = edges.get(sourceIndex);
            for (DAG.Edge<String, String> edge : edgesFromSource) {
                simpleFormEdges.add(new String[]{sourceIndex, edge.i});
            }
        }
        return simpleFormEdges;
    }

    private static Set<String> getSimpleFormNodes(Enumeration e) {
        Set<String> simpleFormNodes = new HashSet<>();
        for (Enumeration iterator = e; iterator.hasMoreElements(); ) {
            simpleFormNodes.add((String)e.nextElement());
        }
        return simpleFormNodes;
    }

}
