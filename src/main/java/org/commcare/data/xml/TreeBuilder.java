package org.commcare.data.xml;

import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.TreeElement;

import java.util.Hashtable;
import java.util.List;

/**
 * Class to build a TreeElement tree for use with DataInstance classes
 */
public class TreeBuilder {

    /**
     * Build a TreeElement populated with children
     */
    public static TreeElement buildTree(String instanceId, String rootElementName, List<SimpleNode> children) {
        TreeElement root = new TreeElement(rootElementName, 0);
        root.setInstanceName(instanceId);
        root.setAttribute(null, "id", instanceId);
        addChildren(instanceId, root, children);
        return root;
    }

    private static void addChildren(String instanceId, TreeElement parent, List<SimpleNode> children) {
        Hashtable<String, Integer> multiplicities = new Hashtable<>();
        for (SimpleNode node : children) {
            int val;
            String name = node.getName();
            if (multiplicities.containsKey(name)) {
                val = multiplicities.get(name) + 1;
            } else {
                val = 0;
            }
            multiplicities.put(name, val);

            TreeElement element = new TreeElement(name, val);
            element.setInstanceName(instanceId);
            node.getAttributes().forEach((attributeName, value) -> {
                element.setAttribute(null, attributeName, value);
            });
            if (node.getValue() != null) {
                element.setValue(new UncastData(node.getValue()));
            } else if (node.getChildren() != null) {
                addChildren(instanceId, element, node.getChildren());
            }
            parent.addChild(element);
        }
    }
}
