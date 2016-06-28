package org.javarosa.core.model.instance.test;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.FormLoadingUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * DataInstance methods tests
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */

public class DataInstanceTest {

    private static final String formPath = "/test_xpathpathexpr.xml";

    @Test
    public void testDataInstance() {
        // load the xml doc into a form instance
        FormInstance model = null;
        try {
            model = FormLoadingUtils.loadFormInstance(formPath);
        } catch (IOException e) {
            fail("Unable to load form at " + formPath);
        } catch (InvalidStructureException e) {
            fail("Form at " + formPath + " has an invalid structure.");
        }

        EvaluationContext eval_ctx = new EvaluationContext(model);

        // make sure a valid path can be found even when the xml sub-elements
        // aren't homogeneous in structure
        assertTrue("Homogeneous template path for a reference",
                model.hasTemplatePath(exprToRef("/data/places/country[1]/name", eval_ctx)));

        assertTrue("Heterogeneous template path for a reference",
                model.hasTemplatePath(exprToRef("/data/places/country[1]/state[0]", eval_ctx)));

        assertTrue("Unfound template path for a reference",
                !model.hasTemplatePath(exprToRef("/data/places/fake[1]/name", eval_ctx)));

        assertTrue("Unfound template path for a reference",
                !model.hasTemplatePath(exprToRef("/data/places/country[1]/fake", eval_ctx)));
    }

    /**
     * Evaluate an xpath query expression into a reference.
     *
     * @param expr     xpath expression
     * @param eval_ctx contextual information needed to evaluate the expression
     */
    public TreeReference exprToRef(String expr, EvaluationContext eval_ctx) {
        XPathPathExpr xpe = null;
        try {
            xpe = (XPathPathExpr)XPathParseTool.parseXPath(expr);
        } catch (XPathSyntaxException xpse) {
        }

        if (xpe == null) {
            fail("Null expression or syntax error " + expr);
            return null;
        }

        TreeReference ref = null;
        try {
            TreeReference genericRef = xpe.getReference();
            if (genericRef.getContext() == TreeReference.CONTEXT_ORIGINAL) {
                ref = genericRef.contextualize(eval_ctx.getOriginalContext());
            } else {
                ref = genericRef.contextualize(eval_ctx.getContextRef());
            }
        } catch (XPathException xpex) {
            fail("Did not get expected exception type");
        }
        return ref;
    }
}
