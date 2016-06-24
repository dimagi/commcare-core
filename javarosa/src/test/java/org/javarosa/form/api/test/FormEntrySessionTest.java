package org.javarosa.form.api.test;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryAction;
import org.javarosa.form.api.FormEntrySession;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Vector;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class FormEntrySessionTest {
    @Test
    public void testSplitByParens() {
        String input = "(a) (b c) (d)";
        Vector<String> expectedOutput = new Vector<>(Arrays.asList("(a)", "(b c)", "(d)"));
        Assert.assertEquals(expectedOutput, FormEntrySession.splitTopParens(input));

        input = "(a) ((b) (c)) ((d))";
        expectedOutput = new Vector<>(Arrays.asList("(a)", "((b) (c))", "((d))"));
        Assert.assertEquals(expectedOutput, FormEntrySession.splitTopParens(input));

        input = "(a) (\\(b c\\)) (\\(d\\))";
        expectedOutput = new Vector<>(Arrays.asList("(a)", "(\\(b c\\))", "(\\(d\\))"));
        Assert.assertEquals(expectedOutput, FormEntrySession.splitTopParens(input));
    }
}
