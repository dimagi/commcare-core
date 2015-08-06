package org.javarosa.core.util;

import java.util.Enumeration;
import java.util.Hashtable;
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

    Hashtable<I, N> nodes;
    Hashtable<I, Vector<Edge<I, E>>> edge;
    Hashtable<I, Vector<Edge<I, E>>> inverse;

    public DAG() {
        nodes = new Hashtable<I, N>();
        edge = new Hashtable<I, Vector<Edge<I, E>>>();
        inverse = new Hashtable<I, Vector<Edge<I, E>>>();
    }

    public void addNode(I i, N n) {
        nodes.put(i, n);
    }

    /**
     * Connect Source -> Destination
     */
    public void setEdge(I source, I destination, E edgeData) {
        addToEdge(edge, source, destination, edgeData);
        addToEdge(inverse, destination, source, edgeData);
    }

    private void addToEdge(Hashtable<I, Vector<Edge<I, E>>> edgeList, I a, I b, E edgeData) {
        Vector<Edge<I, E>> edge;
        if (edgeList.containsKey(a)) {
            edge = edgeList.get(a);
        } else {
            edge = new Vector<Edge<I, E>>();
        }
        edge.addElement(new Edge<I, E>(b, edgeData));
        edgeList.put(a, edge);
    }

    public Vector<Edge<I, E>> getParents(I index) {
        if (inverse.containsKey(index)) {
            return inverse.get(index);
        } else {
            return new Vector();
        }
    }

    public Vector<Edge<I, E>> getChildren(I index) {
        if (!edge.containsKey(index)) {
            return new Vector();
        } else {
            return edge.get(index);
        }
    }

    public N getNode(I index) {
        return nodes.get(index);
    }

    //Is that the right name?

    /**
     * @return Indices for all nodes in the graph which are not the target of
     * any edges in the graph
     */
    public Stack<I> getSources() {
        Stack<I> sources = new Stack();
        for (Enumeration en = nodes.keys(); en.hasMoreElements(); ) {
            I i = (I)en.nextElement();
            if (!inverse.containsKey(i)) {
                sources.addElement(i);
            }
        }
        return sources;
    }

    //Is that the right name?

    /**
     * @return Indices for all nodes that do not have any outgoing edges
     */
    public Stack<I> getSinks() {
        Stack<I> roots = new Stack();
        for (Enumeration en = nodes.keys(); en.hasMoreElements(); ) {
            I i = (I)en.nextElement();
            if (!edge.containsKey(i)) {
                roots.addElement(i);
            }
        }
        return roots;
    }

    public static class Edge<I, E> {
        public I i;
        public E e;

        public Edge(I i, E e) {
            this.i = i;
            this.e = e;
        }
    }

    public Enumeration getNodes() {
        return nodes.elements();
    }
}
