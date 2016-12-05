package org.javarosa.xpath.parser.ast;

import org.javarosa.core.model.condition.HashRefResolver;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFilterExpr;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.expr.XPathStep;
import org.javarosa.xpath.parser.Token;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ASTNodeLocPath extends ASTNode {
    public final List<ASTNode> clauses;
    public List<Integer> separators;

    public ASTNodeLocPath() {
        clauses = new ArrayList<>();
        separators = new ArrayList<>();
    }

    @Override
    public List<? extends ASTNode> getChildren() {
        return clauses;
    }

    public boolean isAbsolute() {
        return clauses.size() == separators.size()
                || (clauses.size() == 0 && separators.size() == 1)
                || isHashRef();
    }

    private boolean isHashRef() {
        return !clauses.isEmpty()
                && clauses.get(0) instanceof ASTNodePathStep
                && ((ASTNodePathStep)clauses.get(0)).nodeTestType == ASTNodePathStep.NODE_TEST_TYPE_HASH_REF;
    }

    @Override
    public XPathExpression build(HashRefResolver hashRefResolver) throws XPathSyntaxException {
        ArrayList<XPathStep> steps = new ArrayList<>();
        XPathExpression filtExpr = null;
        int offset = isAbsolute() ? 1 : 0;
        for (int i = 0; i < clauses.size() + offset; i++) {
            if (offset == 0 || i > 0) {
                ASTNode currentClause = clauses.get(i - offset);
                if (currentClause instanceof ASTNodePathStep) {
                    steps.add(((ASTNodePathStep)currentClause).getStep(hashRefResolver));
                } else {
                    filtExpr = currentClause.build(hashRefResolver);
                }
            }

            if (i < separators.size()) {
                if (separators.get(i) == Token.DBL_SLASH) {
                    steps.add(XPathStep.ABBR_DESCENDANTS());
                }
            }
        }

        XPathStep[] stepArr = steps.toArray(new XPathStep[]{});
        if (filtExpr == null) {
            if (isAbsolute()) {
                if (isHashRef()) {
                    if (hashRefResolver == null) {
                        return XPathPathExpr.buildHashRefPath(stepArr);
                    } else {
                        XPathStep[] resolvedHashSteps =  hashRefResolver.resolveLetRefPathSteps(stepArr[0]);
                        if (resolvedHashSteps == null) {
                            return XPathPathExpr.buildHashRefPath(stepArr);
                        } else {
                            XPathStep[] result = Arrays.copyOf(resolvedHashSteps, resolvedHashSteps.length + stepArr.length - 1);
                            System.arraycopy(stepArr, 1, result, resolvedHashSteps.length, stepArr.length - 1);
                            return XPathPathExpr.buildAbsolutePath(result);
                        }
                    }
                } else {
                    return XPathPathExpr.buildAbsolutePath(stepArr);
                }
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
