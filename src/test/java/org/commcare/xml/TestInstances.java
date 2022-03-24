package org.commcare.xml;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.commcare.data.xml.SimpleNode;
import org.commcare.data.xml.TreeBuilder;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.VirtualDataInstance;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class TestInstances {

    private static final String SELECTED_CASES = "selected-cases";
    private static final String SESSION = "session";
    private static final String CASEDB = "casedb";

    public static Hashtable<String, DataInstance> getInstances() {
        Hashtable<String, DataInstance> instances = new Hashtable<>();
        instances.put(SESSION, buildSessionInstance());
        instances.put(SELECTED_CASES, buildSelectedCases());
        instances.put(CASEDB, buildCaseDb());
        return instances;
    }

    public static VirtualDataInstance buildSessionInstance() {
        List<SimpleNode> nodes = ImmutableList.of(
                SimpleNode.textNode("case_id", Collections.emptyMap(), "bang")
        );
        TreeElement root = TreeBuilder.buildTree(SESSION, SESSION, nodes);
        return new VirtualDataInstance(SESSION, root);
    }

    public static VirtualDataInstance buildSelectedCases() {
        List<SimpleNode> nodes = ImmutableList.of(
                SimpleNode.textNode("value", Collections.emptyMap(), "123"),
                SimpleNode.textNode("value", Collections.emptyMap(), "456"),
                SimpleNode.textNode("value", Collections.emptyMap(), "789")
        );
        TreeElement root = TreeBuilder.buildTree(SELECTED_CASES, "session-data", nodes);
        return new VirtualDataInstance(SELECTED_CASES, root);
    }

    public static VirtualDataInstance buildCaseDb() {
        List<SimpleNode> nodes = ImmutableList.of(
                SimpleNode.textNode("case", ImmutableMap.of("case_id", "123"), "123")
        );
        TreeElement root = TreeBuilder.buildTree(CASEDB, CASEDB, nodes);
        return new VirtualDataInstance(CASEDB, root);
    }
}
