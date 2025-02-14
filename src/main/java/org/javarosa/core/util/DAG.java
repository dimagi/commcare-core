package org.javarosa.core.util;

import org.commcare.modern.util.Pair;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

/**
 * Directed A-cyclic (NOT ENFORCED) graph datatype.
 *
 * Genericized with three types: An unique index value (representing the node), a generic
 * set of data to associate with that node, and a piece of data to associate with each
 * edge
 *
 * @author ctsims
 */
public class DAG<I, N, E> {
    //TODO: This is a really unsafe datatype. Needs an absurd amount of updating for representation
    //invariance, synchronicity, cycle detection, etc.

    private final Hashtable<I, N> nodes;
    private final Hashtable<I, Vector<Edge<I, E>>> edges;
    private final Hashtable<I, Vector<Edge<I, E>>> inverseEdges;

    public DAG() {
        nodes = new Hashtable<>();
        edges = new Hashtable<>();
        inverseEdges = new Hashtable<>();
    }

    public void addNode(I i, N n) {
        nodes.put(i, n);
    }

    public N removeNode(I i) {
        return nodes.remove(i);
    }

    /**
     * Connect Source -> Destination
     */
    public void setEdge(I source, I destination, E edgeData) {
        addToEdges(edges, source, destination, edgeData);
        addToEdges(inverseEdges, destination, source, edgeData);
    }

    private void addToEdges(Hashtable<I, Vector<Edge<I, E>>> edgeList, I source, I dest, E edgeData) {
        Vector<Edge<I, E>> edge;
        if (edgeList.containsKey(source)) {
            edge = edgeList.get(source);
        } else {
            edge = new Vector<>();
        }
        edge.addElement(new Edge<>(dest, edgeData));
        edgeList.put(source, edge);
    }

    /**
     * Removes the given edge from both edge lists
     */
    public void removeEdge(I sourceIndex, I destinationIndex) {
        removeEdge(edges, sourceIndex, destinationIndex);
        removeEdge(inverseEdges, destinationIndex, sourceIndex);
    }

    /**
     * If an edge from sourceIndex to destinationIndex exists in the given edge list, remove it
     */
    private void removeEdge(Hashtable<I, Vector<Edge<I, E>>> edgeList,
                            I sourceIndex,
                            I destinationIndex) {
        Vector<Edge<I, E>> edgesFromSource = edgeList.get(sourceIndex);
        if (edgesFromSource != null) {
            for (Edge<I, E> edge : edgesFromSource) {
                if (edge.i.equals(destinationIndex)) {
                    // Remove the edge
                    edgesFromSource.removeElement(edge);

                    // If removing this edge has made it such that this source index no longer has
                    // any edges from it, remove that entire index from the edges hashtable
                    if (edgesFromSource.size() == 0) {
                        edgeList.remove(sourceIndex);
                    }

                    return;
                }
            }
        }
    }

    public Vector<Edge<I, E>> getParents(I index) {
        if (inverseEdges.containsKey(index)) {
            return inverseEdges.get(index);
        } else {
            return new Vector<>();
        }
    }

    public Vector<Edge<I, E>> getChildren(I index) {
        if (!edges.containsKey(index)) {
            return new Vector<>();
        } else {
            return edges.get(index);
        }
    }

    public N getNode(I index) {
        return nodes.get(index);
    }

    /**
     * @return Indices for all nodes in the graph which are not the target of
     * any edges in the graph
     */
    public Stack<I> getSources() {
        Stack<I> sources = new Stack<>();
        for (Enumeration en = nodes.keys(); en.hasMoreElements(); ) {
            I i = (I)en.nextElement();
            if (!inverseEdges.containsKey(i)) {
                sources.addElement(i);
            }
        }
        return sources;
    }

    /**
     * @return Indices for all nodes that do not have any outgoing edges
     */
    public Stack<I> getSinks() {
        Stack<I> roots = new Stack<>();
        for (Enumeration en = nodes.keys(); en.hasMoreElements(); ) {
            I i = (I)en.nextElement();
            if (!edges.containsKey(i)) {
                roots.addElement(i);
            }
        }
        return roots;
    }

    public Set<I> getRelatedRecords(Set<I> recordIds) {
        Set<I> visited = new HashSet<>();
        LinkedList<I> queue = new LinkedList<>(recordIds);
        while (!queue.isEmpty()) {
            I current = queue.poll();
            if(visited.contains(current)){
                continue;
            }
            visited.add(current);
            addNeighbors(edges, current, queue, visited);
            addNeighbors(inverseEdges, current, queue, visited);
        }
        return visited;
    }

    private void addNeighbors(Hashtable<I, Vector<Edge<I, E>>> edges, I current, LinkedList<I> queue,
            Set<I> visited) {
        if (edges.containsKey(current)) {
            Vector<Edge<I, E>> neighbors = edges.get(current);
            for (Edge<I, E> neighbor : neighbors) {
                if(!visited.contains(neighbor.i)){
                    queue.add(neighbor.i);
                }
            }
        }
    }

    public static class Edge<I, E> {
        public final I i;
        public final E e;

        public Edge(I i, E e) {
            this.i = i;
            this.e = e;
        }
    }

    public Enumeration getNodes() {
        return nodes.elements();
    }

    public int getNodesCount() {
        return nodes.size();
    }

    public Enumeration getIndices() {
        return nodes.keys();
    }

    public Hashtable<I, Vector<Edge<I, E>>> getEdges() {
        return this.edges;
    }

    public boolean containsCycle() {
        HashSet<I> visited = new HashSet();
        HashSet<I> currentPath = new HashSet<>();

        for (I i : nodes.keySet()) {
            if (nodePathContainsCycle(i, visited, currentPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean nodePathContainsCycle(I i, HashSet<I> visited, HashSet<I> currentPath) {
        if (!visited.contains(i)) {
            visited.add(i);
            currentPath.add(i);

            if(edges.containsKey(i)) {
                for (Edge<I, E> e : edges.get(i)) {
                    if (!visited.contains(e.i) && nodePathContainsCycle(e.i, visited, currentPath)) {
                        return true;
                    } else if (currentPath.contains(e.i)) {
                        return true;
                    }
                }
            }
        }
        currentPath.remove(i);
        return false;
    }
}
