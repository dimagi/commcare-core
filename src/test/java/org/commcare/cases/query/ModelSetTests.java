package org.commcare.cases.query;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ctsims on 2/6/2017.
 */

public class ModelSetTests {

    @Test
    public void testCaseParentMatch() throws Exception {
        TreeReference caseRootRef =
                ((XPathPathExpr)XPathParseTool.parseXPath("instance('casedb')/casedb/case")).getReference();

        TreeReference querySetOptimizedLookup =
                ((XPathPathExpr)XPathParseTool.parseXPath("instance('casedb')/casedb/case[@case_id = current()/index/host]/value")).getReference();

        Assert.assertTrue("Parent Reference isn't identified", caseRootRef.isParentOf(querySetOptimizedLookup, false));
    }

    @Test
    public void testCaseIndexMatch() throws Exception {
        TreeReference root = XPathReference.getPathExpr("instance('casedb')/casedb/case").getReference();

        TreeReference member = XPathReference.getPathExpr("instance('casedb')/casedb/case").getReference();
        member.setMultiplicity(member.size() - 1, 10);

        Assert.assertEquals("Contextualized reference", member.genericizeAfter(member.size() - 1), root);

    }
}
