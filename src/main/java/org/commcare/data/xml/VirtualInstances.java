package org.commcare.data.xml;

import static org.javarosa.core.model.instance.ExternalDataInstance.JR_REMOTE_REFERENCE;
import static org.javarosa.core.model.instance.ExternalDataInstance.JR_SEARCH_INPUT_REFERENCE;
import static org.javarosa.core.model.instance.ExternalDataInstance.JR_SELECTED_ENTITIES_REFERENCE;

import com.google.common.collect.ImmutableMap;

import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VirtualInstances {

    public static final String SEARCH_INSTANCE_ROOT_NAME = "input";
    public static final String SEARCH_INSTANCE_NODE_NAME = "field";
    public static final String SEARCH_INPUT_NODE_NAME_ATTR = "name";

    public static final String SELCTED_CASES_INSTANCE_ROOT_NAME = "results";
    public static final String SELCTED_CASES_INSTANCE_NODE_NAME = "value";

    public static String makeSearchInputInstanceID(String suffix) {
        return String.format("search-input:%s", suffix);
    }

    public static ExternalDataInstance buildSearchInputInstance(
            String refId, Map<String, String> userInputValues) {
        List<SimpleNode> nodes = new ArrayList<>();
        userInputValues.forEach((key, value) -> {
            Map<String, String> attributes = ImmutableMap.of(SEARCH_INPUT_NODE_NAME_ATTR, key);
            nodes.add(SimpleNode.textNode(SEARCH_INSTANCE_NODE_NAME, attributes, value));
        });
        String instanceId = makeSearchInputInstanceID(refId);
        TreeElement root = TreeBuilder.buildTree(instanceId, SEARCH_INSTANCE_ROOT_NAME, nodes);
        return new ExternalDataInstance(getSearchInputReference(refId), instanceId, root);
    }

    public static ExternalDataInstance buildSelectedValuesInstance(
            String instanceId, String[] selectedValues) {
        List<SimpleNode> nodes = new ArrayList<>();
        for (String selectedValue : selectedValues) {
            nodes.add(SimpleNode.textNode(SELCTED_CASES_INSTANCE_NODE_NAME, selectedValue));
        }
        TreeElement root = TreeBuilder.buildTree(instanceId, SELCTED_CASES_INSTANCE_ROOT_NAME,
                nodes);
        return new ExternalDataInstance(getSelectedEntitiesReference(instanceId), instanceId, root);
    }


    public static String getSelectedEntitiesReference(String referenceId) {
        return getInstanceReference(JR_SELECTED_ENTITIES_REFERENCE, referenceId);
    }

    public static String getSearchInputReference(String referenceId) {
        return getInstanceReference(JR_SEARCH_INPUT_REFERENCE, referenceId);
    }

    public static String getRemoteReference(String referenceId) {
        return getInstanceReference(JR_REMOTE_REFERENCE, referenceId);
    }

    /**
     * Parses instance reference of format "refScheme/refId" to return the reference id
     *
     * @param reference An instance reference in form of "refScheme/refId"
     * @return reference id from the given reference
     */
    public static String getReferenceId(String reference) {
        return reference.substring(reference.lastIndexOf('/') + 1);
    }

    /**
     * Parses instance reference of format "refScheme/refId" to return the reference scheme
     *
     * @param reference An instance reference in form of "refScheme/refId"
     * @return reference scheme from the given reference
     */
    public static String getReferenceScheme(String reference) {
        return reference.substring(0, reference.lastIndexOf('/'));
    }

    /**
     * Constructs an instance reference in format "refScheme/refId"
     *
     * @param referenceScheme reference scheme for the instance reference
     * @param referenceId     reference id for the instance reference
     * @return an instance reference in format "refScheme/refId"
     */
    public static String getInstanceReference(String referenceScheme, String referenceId) {
        return referenceScheme.concat("/").concat(referenceId);
    }
}
