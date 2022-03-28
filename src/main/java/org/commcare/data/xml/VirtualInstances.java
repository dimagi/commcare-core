package org.commcare.data.xml;

import com.google.common.collect.ImmutableMap;

import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.VirtualDataInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VirtualInstances {
    public static final String SEARCH_INSTANCE_ID = "search-input";
    public static final String SEARCH_INSTANCE_ROOT_NAME = "input";
    public static final String SEARCH_INSTANCE_NODE_NAME = "field";
    public static final String SEARCH_INPUT_NODE_NAME_ATTR = "name";

    public static VirtualDataInstance buildSearchInputInstance(Map<String, String> userInputValues) {
        List<SimpleNode> nodes = new ArrayList<>();
        userInputValues.forEach((key, value) -> {
            Map<String, String> attributes = ImmutableMap.of(SEARCH_INPUT_NODE_NAME_ATTR, key);
            nodes.add(SimpleNode.textNode(SEARCH_INSTANCE_NODE_NAME, attributes, value));
        });
        TreeElement root = TreeBuilder.buildTree(SEARCH_INSTANCE_ID, SEARCH_INSTANCE_ROOT_NAME, nodes);
        return new VirtualDataInstance(SEARCH_INSTANCE_ID, root);
    }
}