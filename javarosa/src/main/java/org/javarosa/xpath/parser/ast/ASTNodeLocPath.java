package org.javarosa.xpath.parser.ast;

import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFilterExpr;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.expr.XPathStep;
import org.javarosa.xpath.parser.Parser;
import org.javarosa.xpath.parser.Token;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.Vector;

public class ASTNodeLocPath extends ASTNode {
    public final Vector<ASTNode> clauses;
    public Vector<Integer> separators;

    public ASTNodeLocPath() {
        clauses = new Vector<>();
        separators = new Vector<>();
    }

    public Vector getChildren() {
        return clauses;
    }

    public boolean isAbsolute() {
        return (clauses.size() == separators.size()) || (clauses.size() == 0 && separators.size() == 1);
    }

    public XPathExpression build() throws XPathSyntaxException {
        ArrayList<XPathStep> steps = new ArrayList<>();
        XPathExpression filtExpr = null;
        int offset = isAbsolute() ? 1 : 0;
        for (int i = 0; i < clauses.size() + offset; i++) {
            if (offset == 0 || i > 0) {
                if (clauses.elementAt(i - offset) instanceof ASTNodePathStep) {
                    steps.add(((ASTNodePathStep)clauses.elementAt(i - offset)).getStep());
                } else {
                    filtExpr = clauses.elementAt(i - offset).build();
                }
            }

            if (i < separators.size()) {
                if (Parser.vectInt(separators, i) == Token.DBL_SLASH) {
                    steps.add(XPathStep.ABBR_DESCENDANTS());
                }
            }
        }

        XPathStep[] stepArr = steps.toArray(new XPathStep[]{});
        if (filtExpr == null) {
            if (isAbsolute()) {
                return XPathPathExpr.buildAbsolutePath(stepArr);
            } else {
                return XPathPathExpr.buildRelativePath(stepArr);
            }
        } else {
            if (filtExpr instanceof XPathFilterExpr) {
                return XPathPathExpr.buildFilterPath((XPathFilterExpr)filtExpr, stepArr);
            } else {
                return XPathPathExpr.buildFilterPath(new XPathFilterExpr(filtExpr, new XPathExpression[0]), stepArr);
            }
        }
    }
}
