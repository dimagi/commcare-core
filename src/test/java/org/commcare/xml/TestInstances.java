package org.commcare.xml;

import static org.javarosa.core.model.instance.ExternalDataInstance.JR_CASE_DB_REFERENCE;
import static org.javarosa.core.model.instance.ExternalDataInstance.JR_SELECTED_VALUES_REFERENCE;
import static org.javarosa.core.model.instance.ExternalDataInstance.JR_SESSION_REFERENCE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.commcare.data.xml.SimpleNode;
import org.commcare.data.xml.TreeBuilder;
import org.commcare.data.xml.VirtualInstances;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

/**
 * Utilities for building mock {@link DataInstance} objects for tests.
 */
public class TestInstances {

    private static final String SELECTED_CASES = "selected-cases";
    private static final String SESSION = "session";
    public static final String CASEDB = "casedb";

    public static Hashtable<String, DataInstance> getInstances() {
        Hashtable<String, DataInstance> instances = new Hashtable<>();
        instances.put(SESSION, buildSessionInstance());
        instances.put(SELECTED_CASES, buildSelectedCases());
        instances.put(CASEDB, buildCaseDb());
        return instances;
    }

    public static ExternalDataInstance buildSessionInstance() {
        List<SimpleNode> nodes = ImmutableList.of(
                SimpleNode.textNode("case_id", Collections.emptyMap(), "bang")
        );
        TreeElement root = TreeBuilder.buildTree(SESSION, SESSION, nodes);
        return new ExternalDataInstance(JR_SESSION_REFERENCE, SESSION, root);
    }

    public static ExternalDataInstance buildSelectedCases() {
        List<SimpleNode> nodes = ImmutableList.of(
                SimpleNode.textNode("value", Collections.emptyMap(), "123"),
                SimpleNode.textNode("value", Collections.emptyMap(), "456"),
                SimpleNode.textNode("value", Collections.emptyMap(), "789")
        );
        TreeElement root = TreeBuilder.buildTree(SELECTED_CASES, "session-data", nodes);
        return new ExternalDataInstance(JR_SELECTED_VALUES_REFERENCE, SELECTED_CASES, root);
    }

    public static ExternalDataInstance buildCaseDb() {
        List<SimpleNode> nodes = ImmutableList.of(
                SimpleNode.textNode("case", ImmutableMap.of("case_id", "123"), "123")
        );
        TreeElement root = TreeBuilder.buildTree(CASEDB, CASEDB, nodes);
        return new ExternalDataInstance(JR_CASE_DB_REFERENCE, CASEDB, root);
    }

    public static ExternalDataInstance buildCaseDb(List<String> caseIds) {
        List<SimpleNode> nodes = new ArrayList<>();
        caseIds.forEach(caseId -> {
            nodes.add(SimpleNode.textNode("case", ImmutableMap.of("case_id", caseId), caseId));
        });
        TreeElement root = TreeBuilder.buildTree(CASEDB, CASEDB, nodes);
        return new ExternalDataInstance(JR_CASE_DB_REFERENCE, CASEDB, root);
    }
}
