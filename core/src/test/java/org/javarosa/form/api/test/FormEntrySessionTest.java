package org.javarosa.form.api.test;

import org.javarosa.core.model.FormIndex;
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

    @Test
    public void testFormEntrySessionStringSerialization() {
        FormEntrySession formEntrySession = new FormEntrySession();
        formEntrySession.addValueSet(FormIndex.createBeginningOfFormIndex(), "foo");
        formEntrySession.addNewRepeat(FormIndex.createBeginningOfFormIndex());
        formEntrySession.addValueSet(FormIndex.createEndOfFormIndex(), "bar");
        String sessionAsString = formEntrySession.toString();
        Assert.assertEquals(sessionAsString, FormEntrySession.fromString(sessionAsString).toString());
    }

    @Test
    public void testAddingDuplicate() {
        FormEntrySession formEntrySession = new FormEntrySession();
        formEntrySession.addValueSet(FormIndex.createBeginningOfFormIndex(), "foo");
        formEntrySession.addValueSet(FormIndex.createBeginningOfFormIndex(), "foo");
        Assert.assertEquals(1, formEntrySession.size());

        FormIndex randomIndex = new FormIndex(FormIndex.createBeginningOfFormIndex(), 52, 2, null);
        formEntrySession.addValueSet(randomIndex, "bar");
        FormEntrySession.FormEntryAction secondAction = formEntrySession.peekAction();

        formEntrySession.addValueSet(FormIndex.createBeginningOfFormIndex(), "foo");

        Assert.assertEquals(2, formEntrySession.size());
        Assert.assertEquals(secondAction, formEntrySession.peekAction());
    }
}
