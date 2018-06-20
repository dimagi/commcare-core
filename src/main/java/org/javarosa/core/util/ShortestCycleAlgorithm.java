package org.javarosa.core.util;

import org.javarosa.core.model.instance.TreeReference;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
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
    ArrayList<String> nodes = new ArrayList<String>();
    Hashtable<String, ArrayList<String>> childrenMap = new OrderedHashtable<String, ArrayList<String>>();
    ArrayList<String> shortestCycle = null;
    Hashtable<String, ArrayList<String>> reachableMap = new OrderedHashtable<String, ArrayList<String>>();
    ArrayList<String> walked = new ArrayList<String>();

    public ShortestCycleAlgorithm(Vector<TreeReference[]> edges) {
        this.edges = edges;
        for (TreeReference[] references: edges) {
            String parentKey = references[0].toString();
            String childKey = references[1].toString();
            addChild(parentKey, childKey);
            if (!nodes.contains(parentKey)) {
                nodes.add(parentKey);
            }
        }

        for (String node: nodes) {
            ArrayList<String> shortestPath = depthFirstSearch(node, node, new ArrayList<String>());
            if (shortestPath != null && (shortestCycle == null || shortestPath.size() < shortestCycle.size())) {
                shortestCycle = shortestPath;
            }
        }
    }

    private void addChild(String parentKey, String childKey) {
        if (!childrenMap.containsKey(parentKey)) {
            childrenMap.put(parentKey, new ArrayList<String>());
        }
        ArrayList<String> childList = childrenMap.get(parentKey);
        if (!childList.contains(childKey)) {
            childList.add(childKey);
        }
    }

    // Add the new node to the set of reachable nodes for all already-visited nodes
    private void addReachbleToVisited(List<String> visited, String reachable) {
        for (String visit: visited) {
            addReachable(visit, reachable);
        }
    }

    private void addReachable(String parent, String reachable) {
        if (!reachableMap.containsKey(parent)) {
            reachableMap.put(parent, new ArrayList<String>());
        }
        ArrayList<String> reachableList = reachableMap.get(parent);
        if (!reachableList.contains(reachable)) {
            reachableList.add(reachable);
        }
    }

    private ArrayList<String> depthFirstSearch(String startNode, String currentNode, ArrayList<String> visited) {
        addReachbleToVisited(visited, currentNode);
        if (visited.contains(currentNode)) {
            if (startNode.equals(currentNode)) {
                return visited;
            }
            return null;
        }

        visited.add(currentNode);
        ArrayList<String> children = childrenMap.get(currentNode);
        if (children != null) {
            for (String child : children) {

                // If we have already walked this node, get the set of reachable nodes from that walk
                // If that set does not contain any of the visited nodes in the current walk
                // Then this child cannot contain a cycle
                if (walked.contains(child)) {
                    ArrayList<String> reachables = reachableMap.get(child);
                    if (reachables == null || !reachables.contains(visited)) {
                        continue;
                    }
                }

                ArrayList<String> shortestPath = depthFirstSearch(startNode, child, visited);
                if (shortestPath != null) {
                    return shortestPath;
                }
            }
        }
        walked.add(currentNode);
        visited.remove(currentNode);
        return null;
    }

    public String getCycleErrorMessage() {
        return "Logic is cyclical, referencing itself. Shortest Cycle: \n" + getCycleString();
    }

    public String getCycleString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < shortestCycle.size(); i++) {
            stringBuilder.append(shortestCycle.get(i));
            if (i == shortestCycle.size() - 1) {
                stringBuilder.append(" is referenced by ");
                stringBuilder.append(shortestCycle.get(0));
                stringBuilder.append(".");
            } else {
                stringBuilder.append( " is referenced by " );
                stringBuilder.append(shortestCycle.get(i + 1));
                stringBuilder.append( ", \n" );
            }
        }
        return stringBuilder.toString();
    }
}
