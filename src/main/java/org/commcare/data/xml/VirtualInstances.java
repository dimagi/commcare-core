package org.commcare.data.xml;

import com.google.common.collect.ImmutableMap;

import org.javarosa.core.model.instance.ExternalDataInstance;
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

    public static final String SELCTED_CASES_INSTANCE_ROOT_NAME = "results";
    public static final String SELCTED_CASES_INSTANCE_NODE_NAME = "value";

    public final static String JR_SEARCH_INPUT_REFERENCE = "jr://instance/search_input";
    public final static String JR_SELECTED_VALUES_REFERENCE = "jr://instance/selected_cases";

    public static ExternalDataInstance buildSearchInputInstance(Map<String, String> userInputValues) {
        List<SimpleNode> nodes = new ArrayList<>();
        userInputValues.forEach((key, value) -> {
            Map<String, String> attributes = ImmutableMap.of(SEARCH_INPUT_NODE_NAME_ATTR, key);
            nodes.add(SimpleNode.textNode(SEARCH_INSTANCE_NODE_NAME, attributes, value));
        });
        TreeElement root = TreeBuilder.buildTree(SEARCH_INSTANCE_ID, SEARCH_INSTANCE_ROOT_NAME, nodes);
        return new VirtualDataInstance(JR_SEARCH_INPUT_REFERENCE, SEARCH_INSTANCE_ID, root);
    }

    public static ExternalDataInstance buildSelectedValuesInstance(
            String instanceId, String[] selectedValues) {
        List<SimpleNode> nodes = new ArrayList<>();
        for (String selectedValue : selectedValues) {
            nodes.add(SimpleNode.textNode(SELCTED_CASES_INSTANCE_NODE_NAME, selectedValue));
        }
        TreeElement root = TreeBuilder.buildTree(instanceId, SELCTED_CASES_INSTANCE_ROOT_NAME,
                nodes);
        return new VirtualDataInstance(JR_SELECTED_VALUES_REFERENCE, instanceId, root);
    }
}
