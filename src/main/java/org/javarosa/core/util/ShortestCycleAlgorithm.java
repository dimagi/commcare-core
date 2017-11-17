package org.javarosa.core.util;

import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * Modeled after algorithm here https://stackoverflow.com/a/549312
 *
 * Given a Vector of TreeReference pairs where edges[0] references edges[1]
 * that we assume to contain a cycle, find the smallest cycle in the set.
 *
 * Created by willpride on 11/17/17.
 */
public class ShortestCycleAlgorithm {

    Vector<TreeReference[]> edges;
    ArrayList<String> nodes = new ArrayList<>();
    Hashtable<String, ArrayList<String>> childrenMap = new OrderedHashtable<>();
    ArrayList<String> shortestCycle = null;

    public ShortestCycleAlgorithm(Vector<TreeReference[]> edges) {
        this.edges = edges;
        for (TreeReference[] references: edges) {
            String parentKey = references[0].toString();
            String childKey = references[1].toString();
            addChild(parentKey, childKey);
            nodes.add(parentKey);
        }

        for (String node: nodes) {
            ArrayList<String> shortestPath = depthFirstSearch(node, node, new ArrayList<>());
            if (shortestPath != null && (shortestCycle == null || shortestPath.size() < shortestCycle.size())) {
                shortestCycle = shortestPath;
            }
        }
    }

    private void addChild(String parentKey, String childKey) {
        if (!childrenMap.containsKey(parentKey)) {
            childrenMap.put(parentKey, new ArrayList<>());
        }
        ArrayList<String> childList = childrenMap.get(parentKey);
        childList.add(childKey);
    }

    private ArrayList<String> depthFirstSearch(String startNode, String currentNode, ArrayList<String> visited) {
        if (visited.contains(currentNode)) {
            if (startNode.equals(currentNode)) {
                return visited;
            }
            return null;
        }
        visited.add(currentNode);
        for (String child: childrenMap.get(currentNode)) {
            ArrayList<String> shortestPath = depthFirstSearch(startNode, child, visited);
            if (shortestPath != null) {
                return shortestPath;
            }
        }
        visited.remove(currentNode);
        return null;
    }

    public ArrayList<String> getCycle() {
        return shortestCycle;
    }
}
