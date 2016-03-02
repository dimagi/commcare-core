package org.javarosa.core.util.test;

import org.javarosa.core.util.PrefixTree;
import org.javarosa.core.util.PrefixTreeNode;
import org.junit.Test;

import static org.junit.Assert.fail;

public class PrefixTreeTest {
    private final int[] prefixLengths = new int[]{0, 1, 2, 10, 50};

    @Test
    public void testBasic() {
        for (int i : prefixLengths) {
            PrefixTree t = new PrefixTree(i);

            add(t, "abcde");
            add(t, "abcdefghij");
            add(t, "abcdefghijklmno");
            add(t, "abcde");
            add(t, "abcdefg");
            add(t, "xyz");
            add(t, "abcdexyz");
            add(t, "abcppppp");
            System.out.println(t.toString());
        }
    }

    @Test
    public void testHeuristic() {
        for (int i : prefixLengths) {

            PrefixTree t = new PrefixTree(i);

            add(t, "jr://file/images/something/abcd.png");
            add(t, "jr://file/audio/something/abcd.mp3");
            add(t, "jr://file/audio/something/adfd.mp3");
            add(t, "jr://file/images/something/dsf.png");
            add(t, "jr://file/images/sooth/abcd.png");
            add(t, "jr://file/audio/something/bsadf.mp3");
            add(t, "jr://file/audio/something/fsde.mp3");

            add(t, "jr://file/images/some");
            System.out.println(t.toString());
        }
    }

    public void add(PrefixTree t, String s) {
        PrefixTreeNode node = t.addString(s);

        if (!node.render().equals(s)) {
            fail("Prefix tree mangled: " + s + " into " + node.render());
        }
    }
}
