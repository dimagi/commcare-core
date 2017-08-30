package org.javarosa.xpath.parser.ast;

import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.Token;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Enumeration;
import java.util.List;

public abstract class ASTNode {
    public abstract List<? extends ASTNode> getChildren();

    public abstract XPathExpression build() throws XPathSyntaxException;

    private int indent;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        translate(this, sb);
        return sb.toString();
    }

    private void translate(Object o, StringBuilder sb) {
        indent += 1;
        if (o instanceof ASTNodeAbstractExpr) {
            ASTNodeAbstractExpr x = (ASTNodeAbstractExpr)o;
            append(sb, "abstractexpr {");
            for (int i = 0; i < x.size(); i++) {
                if (x.getType(i) == ASTNodeAbstractExpr.CHILD)
                    translate(x.content.get(i), sb);
                else
                    append(sb, x.getToken(i).toString());
            }
            append(sb, "}");
        } else if (o instanceof ASTNodePredicate) {
            ASTNodePredicate x = (ASTNodePredicate)o;
            append(sb, "predicate {");
            translate(x.expr, sb);
            append(sb, "}");
        } else if (o instanceof ASTNodeFunctionCall) {
            ASTNodeFunctionCall x = (ASTNodeFunctionCall)o;
            if (x.args.size() == 0) {
                append(sb, "func {" + x.name.toString() + ", args {none}}");
            } else {
                append(sb, "func {" + x.name.toString() + ", args {{");
                for (int i = 0; i < x.args.size(); i++) {
                    translate(x.args.get(i), sb);
                    if (i < x.args.size() - 1)
                        append(sb, " } {");
                }
                append(sb, "}}}");
            }
        } else if (o instanceof ASTNodeBinaryOp) {
            ASTNodeBinaryOp x = (ASTNodeBinaryOp)o;
            append(sb, "opexpr {");
            for (int i = 0; i < x.exprs.size(); i++) {
                translate(x.exprs.get(i), sb);
                if (i < x.exprs.size() - 1) {
                    switch (x.ops.get(i)) {
                        case Token.AND:
                            append(sb, "and:");
                            break;
                        case Token.OR:
                            append(sb, "or:");
                            break;
                        case Token.EQ:
                            append(sb, "eq:");
                            break;
                        case Token.NEQ:
                            append(sb, "neq:");
                            break;
                        case Token.LT:
                            append(sb, "lt:");
                            break;
                        case Token.LTE:
                            append(sb, "lte:");
                            break;
                        case Token.GT:
                            append(sb, "gt:");
                            break;
                        case Token.GTE:
                            append(sb, "gte:");
                            break;
                        case Token.PLUS:
                            append(sb, "plus:");
                            break;
                        case Token.MINUS:
                            append(sb, "minus:");
                            break;
                        case Token.DIV:
                            append(sb, "div:");
                            break;
                        case Token.MOD:
                            append(sb, "mod:");
                            break;
                        case Token.MULT:
                            append(sb, "mult:");
                            break;
                        case Token.UNION:
                            append(sb, "union:");
                            break;
                    }
                }
            }
            append(sb, "}");
        } else if (o instanceof ASTNodeUnaryOp) {
            ASTNodeUnaryOp x = (ASTNodeUnaryOp)o;
            append(sb, "opexpr {");
            switch (x.op) {
                case Token.UMINUS:
                    append(sb, "num-neg:");
                    break;
            }
            translate(x.expr, sb);
            append(sb, "}");
        } else if (o instanceof ASTNodeLocPath) {
            ASTNodeLocPath x = (ASTNodeLocPath)o;
            append(sb, "pathexpr {");
            int offset = x.isAbsolute() ? 1 : 0;
            for (int i = 0; i < x.clauses.size() + offset; i++) {
                if (offset == 0 || i > 0)
                    translate(x.clauses.elementAt(i - offset), sb);
                if (i < x.separators.size()) {
                    switch (x.separators.get(i)) {
                        case Token.DBL_SLASH:
                            append(sb, "dbl-slash:");
                            break;
                        case Token.SLASH:
                            append(sb, "slash:");
                            break;
                    }
                }
            }
            append(sb, "}");

        } else if (o instanceof ASTNodePathStep) {
            ASTNodePathStep x = (ASTNodePathStep)o;
            append(sb, "step {axis: " + x.axisType + " node test type: " + x.nodeTestType);
            if (x.axisType == ASTNodePathStep.AXIS_TYPE_EXPLICIT)
                append(sb, "  axis type: " + x.axisVal);
            if (x.nodeTestType == ASTNodePathStep.NODE_TEST_TYPE_QNAME)
                append(sb, "  node test name: " + x.nodeTestQName.toString());
            if (x.nodeTestType == ASTNodePathStep.NODE_TEST_TYPE_FUNC) translate(x.nodeTestFunc, sb);
            append(sb, "predicates...");
            for (Enumeration e = x.predicates.elements(); e.hasMoreElements(); )
                translate(e.nextElement(), sb);
            append(sb, "}");
        } else if (o instanceof ASTNodeFilterExpr) {
            ASTNodeFilterExpr x = (ASTNodeFilterExpr)o;
            append(sb, "filter expr {");
            translate(x.expr, sb);
            append(sb, "predicates...");
            for (Enumeration e = x.predicates.elements(); e.hasMoreElements(); )
                translate(e.nextElement(), sb);
            append(sb, "}");
        }
        indent -= 1;
    }

    private void append(StringBuilder sb, String s) {
        for (int i = 0; i < 2 * indent; i++)
            sb.append(" ");
        sb.append(s);
    }
}