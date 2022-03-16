package org.commcare.data.xml;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TreeBuilderTest {

    @Test
    public void testBuildTree() throws UnfullfilledRequirementsException, XmlPullParserException, InvalidStructureException, IOException {
        List<SimpleNode> nodes = ImmutableList.of(
            SimpleNode.textNode("node", ImmutableMap.of("a1", "x1", "b1", "y1"), "text1"),
            SimpleNode.textNode("node", ImmutableMap.of("a2", "x2"), "text2"),
            SimpleNode.parentNode("node", Collections.emptyMap(), ImmutableList.of(
                SimpleNode.parentNode("child", Collections.emptyMap(), ImmutableList.of(
                    SimpleNode.textNode("grandchild", Collections.emptyMap(), "text3"),
                    SimpleNode.textNode("grandchild", Collections.emptyMap(), "text4")
                ))
            ))
        );
        TreeElement test = TreeBuilder.buildTree("test-instance", "test", nodes);

        String expectedXml = String.join(
            "",
            "<test id=\"test-instance\">",
                "<node a1=\"x1\" b1=\"y1\">text1</node>",
                "<node a2=\"x2\">text2</node>",
                "<node>",
                    "<child>",
                        "<grandchild>text3</grandchild>",
                        "<grandchild>text4</grandchild>",
                    "</child>",
                "</node>",
            "</test>"
        );
        TreeElement expected = ExternalDataInstance.parseExternalTree(
                new ByteArrayInputStream(expectedXml.getBytes(StandardCharsets.UTF_8)), "test-instance"
        );
        assertEquals(expected, test);
    }
}
