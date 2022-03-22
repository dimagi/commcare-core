package org.commcare.data.xml;

import java.util.List;
import java.util.Map;


/**
 * Metadata class used in conjunction with {@link TreeBuilder} to allow creating simple
 * {@link org.javarosa.core.model.instance.TreeElement} trees.
 *
 * SimpleNode.parentNode("node", ImmutableMap.of("attr1", "v1"), ImmutableList.of(
 * SimpleNode.textNode("child", Collections.emptyMap(), "some text"),
 * SimpleNode.textNode("child", Collections.emptyMap(), "other text")
 * ))
 */
public class SimpleNode {
    private String name;
    private Map<String, String> attributes;
    private List<SimpleNode> children;
    private String value;

    public static SimpleNode textNode(String name, Map<String, String> attributes, String text) {
        return new SimpleNode(name, attributes, text);
    }

    public static SimpleNode parentNode(String name, Map<String, String> attributes, List<SimpleNode> children) {
        return new SimpleNode(name, attributes, children);
    }

    private SimpleNode(String name, Map<String, String> attributes, String text) {
        this.name = name;
        this.attributes = attributes;
        this.value = text;
    }

    private SimpleNode(String name, Map<String, String> attributes, List<SimpleNode> children) {
        this.name = name;
        this.attributes = attributes;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getValue() {
        return value;
    }

    public List<SimpleNode> getChildren() {
        return children;
    }
}
