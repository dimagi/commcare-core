package org.javarosa.xpath.expr.test;

import org.javarosa.xpath.expr.XPathArithExpr;
import org.javarosa.xpath.expr.XPathBoolExpr;
import org.javarosa.xpath.expr.XPathCmpExpr;
import org.javarosa.xpath.expr.XPathEqExpr;
import org.javarosa.xpath.expr.XPathNumericLiteral;
import org.javarosa.xpath.expr.XPathStringLiteral;
import org.javarosa.xpath.expr.XPathUnionExpr;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class XPathBinaryOpExprTest {
    /**
     * Extensive checks of binary op expression equality logic, because we all
     * know 'a > b' isn't the same as 'a = b', but we don't always write code
     * that knows that...
     */
    @Test
    public void equalityForDifferentBinaryOps() {
        XPathStringLiteral leftStringExpr = new XPathStringLiteral("left side");
        XPathNumericLiteral zero = new XPathNumericLiteral(0d);

        // Setup expressions to test equality over.
        // Note: these binary expressions make semantic sense
        XPathArithExpr additionExpr = new XPathArithExpr(XPathArithExpr.ADD, leftStringExpr, zero);
        XPathArithExpr subtractExpr = new XPathArithExpr(XPathArithExpr.SUBTRACT, leftStringExpr, zero);

        XPathBoolExpr andExpr = new XPathBoolExpr(XPathBoolExpr.AND, leftStringExpr, zero);
        XPathBoolExpr orExpr = new XPathBoolExpr(XPathBoolExpr.OR, leftStringExpr, zero);

        XPathCmpExpr lessThanExpr = new XPathCmpExpr(XPathCmpExpr.LT, leftStringExpr, zero);
        XPathCmpExpr greaterThanExpr = new XPathCmpExpr(XPathCmpExpr.GT, leftStringExpr, zero);

        XPathEqExpr eqExpr = new XPathEqExpr(XPathEqExpr.EQ, leftStringExpr, zero);
        XPathEqExpr neqExpr = new XPathEqExpr(XPathEqExpr.NEQ, leftStringExpr, zero);

        XPathUnionExpr union = new XPathUnionExpr(leftStringExpr, zero);
        XPathUnionExpr differentUnion = new XPathUnionExpr(zero, zero);

        // basic equality tests over same subclass
        Assert.assertEquals("Same + expression is equal", additionExpr, additionExpr);
        Assert.assertNotEquals("+ not equal to  -", additionExpr, subtractExpr);
        Assert.assertEquals("Same && expression is equal", andExpr, andExpr);
        Assert.assertNotEquals("&& not equal to ||", andExpr, orExpr);
        Assert.assertEquals("Same < expression is equal", lessThanExpr, lessThanExpr);
        Assert.assertNotEquals("< not equal to  >", lessThanExpr, greaterThanExpr);
        Assert.assertEquals("Same == expression is equal", eqExpr, eqExpr);
        Assert.assertNotEquals("== not equal to !=", eqExpr, neqExpr);

        // make sure different binary expressions with same op code aren't equal
        Assert.assertNotEquals("+ not equal to &&", additionExpr, andExpr);
        Assert.assertNotEquals("+ not equal to <", additionExpr, lessThanExpr);
        Assert.assertNotEquals("+ not equal to ==", additionExpr, eqExpr);
        Assert.assertNotEquals("- not equal to ||", subtractExpr, orExpr);
        Assert.assertNotEquals("- not equal to >", subtractExpr, greaterThanExpr);
        Assert.assertNotEquals("- not equal to !=", subtractExpr, neqExpr);

        // make sure union equality, which doesn't have an op code, works
        Assert.assertEquals("same union instance is equal to itself", union, union);
        Assert.assertNotEquals(union, differentUnion);
        Assert.assertNotEquals("+ not equal to union", additionExpr, union);
    }
}
