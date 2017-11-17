package org.javarosa.core.util;

import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by willpride on 11/17/17.
 */
public class DFS {

    Vector<TreeReference[]> edges;
    ArrayList<String> nodes = new ArrayList<>();
    Hashtable<String, ArrayList<String>> childrenMap = new OrderedHashtable<>();
    ArrayList<String> shortestCycle = null;

    public DFS (Vector<TreeReference[]> edges) {
        this.edges = edges;

        for (TreeReference[] references: edges) {
            String parentKey = references[0].toString();
            String childKey = references[1].toString();
            addChild(parentKey, childKey);
            nodes.add(parentKey);
        }

        for (String node: nodes) {
            ArrayList<String> shortestPath = dfs(node, node, new ArrayList<>());
            if (shortestPath != null && (shortestCycle == null || shortestPath.size() < shortestCycle.size())) {
                shortestCycle = shortestPath;
            }
        }
        System.out.println("Shortest cycle " + shortestCycle);
    }

    private void addChild(String parentKey, String childKey) {
        if (!childrenMap.containsKey(parentKey)) {
            childrenMap.put(parentKey, new ArrayList<>());
        }
        ArrayList<String> childList = childrenMap.get(parentKey);
        childList.add(childKey);
    }

    private ArrayList<String> dfs(String startNode, String currentNode, ArrayList<String> visited) {
        if (visited.contains(currentNode)) {
            if (startNode.equals(currentNode)) {
                return visited;
            }
            return null;
        }
        visited.add(currentNode);
        for (String child: childrenMap.get(currentNode)) {
            ArrayList<String> shortestPath = dfs(startNode, child, visited);
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
