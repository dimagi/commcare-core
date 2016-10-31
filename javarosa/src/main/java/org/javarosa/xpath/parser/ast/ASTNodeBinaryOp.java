package org.javarosa.xpath.parser.ast;

import org.javarosa.xpath.expr.XPathArithExpr;
import org.javarosa.xpath.expr.XPathBinaryOpExpr;
import org.javarosa.xpath.expr.XPathBoolExpr;
import org.javarosa.xpath.expr.XPathCmpExpr;
import org.javarosa.xpath.expr.XPathEqExpr;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathUnionExpr;
import org.javarosa.xpath.parser.Token;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.List;

public class ASTNodeBinaryOp extends ASTNode {
    public static final int ASSOC_LEFT = 1;
    public static final int ASSOC_RIGHT = 2;

    public int associativity;
    public List<? extends ASTNode> exprs;
    public List<Integer> ops;

    public ASTNodeBinaryOp() {
        exprs = new ArrayList<>();
        ops = new ArrayList<>();
    }

    @Override
    public List<? extends ASTNode> getChildren() {
        return exprs;
    }

    @Override
    public XPathExpression build() throws XPathSyntaxException {
        XPathExpression x;

        if (associativity == ASSOC_LEFT) {
            x = exprs.get(0).build();
            for (int i = 1; i < exprs.size(); i++) {
                x = getBinOpExpr(ops.get(i - 1), x, exprs.get(i).build());
            }
        } else {
            x = exprs.get(exprs.size() - 1).build();
            for (int i = exprs.size() - 2; i >= 0; i--) {
                x = getBinOpExpr(ops.get(i), exprs.get(i).build(), x);
            }
        }

        return x;
    }

    private XPathBinaryOpExpr getBinOpExpr(int op, XPathExpression a, XPathExpression b) throws XPathSyntaxException {
        switch (op) {
            case Token.OR:
                return new XPathBoolExpr(XPathBoolExpr.OR, a, b);
            case Token.AND:
                return new XPathBoolExpr(XPathBoolExpr.AND, a, b);
            case Token.EQ:
                return new XPathEqExpr(XPathEqExpr.EQ, a, b);
            case Token.NEQ:
                return new XPathEqExpr(XPathEqExpr.NEQ, a, b);
            case Token.LT:
                return new XPathCmpExpr(XPathCmpExpr.LT, a, b);
            case Token.LTE:
                return new XPathCmpExpr(XPathCmpExpr.LTE, a, b);
            case Token.GT:
                return new XPathCmpExpr(XPathCmpExpr.GT, a, b);
            case Token.GTE:
                return new XPathCmpExpr(XPathCmpExpr.GTE, a, b);
            case Token.PLUS:
                return new XPathArithExpr(XPathArithExpr.ADD, a, b);
            case Token.MINUS:
                return new XPathArithExpr(XPathArithExpr.SUBTRACT, a, b);
            case Token.MULT:
                return new XPathArithExpr(XPathArithExpr.MULTIPLY, a, b);
            case Token.DIV:
                return new XPathArithExpr(XPathArithExpr.DIVIDE, a, b);
            case Token.MOD:
                return new XPathArithExpr(XPathArithExpr.MODULO, a, b);
            case Token.UNION:
                return new XPathUnionExpr(a, b);
            default:
                throw new XPathSyntaxException();
        }
    }
}
