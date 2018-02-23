package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.pivot.UnpivotableExpressionException;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.Logger;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.model.xform.DataModelSerializer;
import org.javarosa.xpath.XPathLazyNodeset;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.analysis.XPathAnalyzable;
import org.kxml2.io.KXmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Vector;

public abstract class XPathExpression implements Externalizable, XPathAnalyzable {

    public Object eval(EvaluationContext evalContext) {
        return eval(evalContext.getMainInstance(), evalContext);
    }

    /**
     * Evaluate this expression, potentially capturing any additional
     * information about the evaluation.
     *
     * @return The result of this expression evaluated against the provided context
     */
    public Object eval(DataInstance model, EvaluationContext evalContext) {
        evalContext.openTrace(this);
        Object value = evalRaw(model, evalContext);
        evalContext.reportTraceValue(value);
        evalContext.closeTrace();
        return value;
    }

    public static void serializeResult(Object value, OutputStream output) throws IOException {
        if (!isLeafNode(value)) {
            serializeElements((XPathNodeset) value, output);
        } else {
            output.write(FunctionUtils.toString(value).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static boolean isLeafNode(Object value) {
        if (!(value instanceof XPathNodeset)) {
            return false;
        }
        XPathNodeset nodeset = (XPathNodeset) value;
        Vector<TreeReference> refs = nodeset.getReferences();
        if (refs == null || refs.size() != 1) {
            return false;
        }

        DataInstance instance = ((XPathLazyNodeset) value).getInstance();
        AbstractTreeElement treeElement = instance.resolveReference(refs.get(0));
        return treeElement.getNumChildren() == 0;
    }

    private static void serializeElements(XPathNodeset nodeset, OutputStream output) throws IOException {
        KXmlSerializer serializer = new KXmlSerializer();

        try {
            serializer.setOutput(output, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DataModelSerializer s = new DataModelSerializer(serializer);

        DataInstance instance = nodeset.getInstance();
        Vector<TreeReference> refs = nodeset.getReferences();
        for (TreeReference ref : refs) {
            AbstractTreeElement treeElement = instance.resolveReference(ref);
            s.serialize(treeElement);
        }
    }

    /**
     * Perform the raw evaluation of this expression producing an
     * appropriately typed XPath output with no side effects
     *
     * @return The result of this expression evaluated against the provided context
     */
    public abstract Object evalRaw(DataInstance model, EvaluationContext evalContext);

    public final Vector<Object> pivot(DataInstance model, EvaluationContext evalContext) throws UnpivotableExpressionException {
        try {
            Vector<Object> pivots = new Vector<>();
            this.pivot(model, evalContext, pivots, evalContext.getContextRef());
            return pivots;
        } catch (UnpivotableExpressionException uee) {
            //Rethrow unpivotable (expected)
            throw uee;
        } catch (Exception e) {
            //Pivots aren't critical, if there was a problem getting one, log the exception
            //so we can fix it, and then just report that.
            Logger.exception(e);
            throw new UnpivotableExpressionException(e.getMessage());
        }
    }

    /**
     * Pivot this expression, returning values if appropriate, and adding any pivots to the list.
     *
     * @param model       The model to evaluate the current expression against
     * @param evalContext The evaluation context to evaluate against
     * @param pivots      The list of pivot points in the xpath being evaluated. Pivots should be added to this list.
     * @param sentinal    The value which is being pivoted around.
     * @return null - If a pivot was identified in this expression
     * sentinal - If the current expression represents the sentinal being pivoted
     * any other value - The result of the expression if no pivots are detected
     * @throws UnpivotableExpressionException If the expression is too complex to pivot
     */
    public Object pivot(DataInstance model, EvaluationContext evalContext, Vector<Object> pivots, Object sentinal) throws UnpivotableExpressionException {
        return eval(model, evalContext);
    }

    /*======= DEBUGGING ========*/
    // should not compile onto phone

    /* print out formatted expression tree */

    private int indent;

    private void printStr(String s) {
        for (int i = 0; i < 2 * indent; i++)
            System.out.print(" ");
        System.out.println(s);
    }

    public void printParseTree() {
        indent = -1;
        print(this);
    }

    public void print(Object o) {
        indent += 1;

        if (o instanceof XPathStringLiteral) {
            XPathStringLiteral x = (XPathStringLiteral)o;
            printStr("strlit {" + x.s + "}");
        } else if (o instanceof XPathNumericLiteral) {
            XPathNumericLiteral x = (XPathNumericLiteral)o;
            printStr("numlit {" + x.d + "}");
        } else if (o instanceof XPathVariableReference) {
            XPathVariableReference x = (XPathVariableReference)o;
            printStr("var {" + x.id.toString() + "}");
        } else if (o instanceof XPathArithExpr) {
            XPathArithExpr x = (XPathArithExpr)o;
            String op = null;
            switch (x.op) {
                case XPathArithExpr.ADD:
                    op = "add";
                    break;
                case XPathArithExpr.SUBTRACT:
                    op = "subtr";
                    break;
                case XPathArithExpr.MULTIPLY:
                    op = "mult";
                    break;
                case XPathArithExpr.DIVIDE:
                    op = "div";
                    break;
                case XPathArithExpr.MODULO:
                    op = "mod";
                    break;
            }
            printStr(op + " {{");
            print(x.a);
            printStr(" } {");
            print(x.b);
            printStr("}}");
        } else if (o instanceof XPathBoolExpr) {
            XPathBoolExpr x = (XPathBoolExpr)o;
            String op = null;
            switch (x.op) {
                case XPathBoolExpr.AND:
                    op = "and";
                    break;
                case XPathBoolExpr.OR:
                    op = "or";
                    break;
            }
            printStr(op + " {{");
            print(x.a);
            printStr(" } {");
            print(x.b);
            printStr("}}");
        } else if (o instanceof XPathCmpExpr) {
            XPathCmpExpr x = (XPathCmpExpr)o;
            String op = null;
            switch (x.op) {
                case XPathCmpExpr.LT:
                    op = "lt";
                    break;
                case XPathCmpExpr.LTE:
                    op = "lte";
                    break;
                case XPathCmpExpr.GT:
                    op = "gt";
                    break;
                case XPathCmpExpr.GTE:
                    op = "gte";
                    break;
            }
            printStr(op + " {{");
            print(x.a);
            printStr(" } {");
            print(x.b);
            printStr("}}");
        } else if (o instanceof XPathEqExpr) {
            XPathEqExpr x = (XPathEqExpr)o;
            String op = x.op == XPathEqExpr.EQ ? "eq" : "neq";
            printStr(op + " {{");
            print(x.a);
            printStr(" } {");
            print(x.b);
            printStr("}}");
        } else if (o instanceof XPathUnionExpr) {
            XPathUnionExpr x = (XPathUnionExpr)o;
            printStr("union {{");
            print(x.a);
            printStr(" } {");
            print(x.b);
            printStr("}}");
        } else if (o instanceof XPathNumNegExpr) {
            XPathNumNegExpr x = (XPathNumNegExpr)o;
            printStr("neg {");
            print(x.a);
            printStr("}");
        } else if (o instanceof XPathFuncExpr) {
            XPathFuncExpr x = (XPathFuncExpr)o;
            if (x.args.length == 0) {
                printStr("func {" + x.name + ", args {none}}");
            } else {
                printStr("func {" + x.name + ", args {{");
                for (int i = 0; i < x.args.length; i++) {
                    print(x.args[i]);
                    if (i < x.args.length - 1)
                        printStr(" } {");
                }
                printStr("}}}");
            }
        } else if (o instanceof XPathPathExpr) {
            XPathPathExpr x = (XPathPathExpr)o;
            String init = null;

            switch (x.initContext) {
                case XPathPathExpr.INIT_CONTEXT_ROOT:
                    init = "root";
                    break;
                case XPathPathExpr.INIT_CONTEXT_RELATIVE:
                    init = "relative";
                    break;
                case XPathPathExpr.INIT_CONTEXT_EXPR:
                    init = "expr";
                    break;
            }

            printStr("path {init-context:" + init + ",");

            if (x.initContext == XPathPathExpr.INIT_CONTEXT_EXPR) {
                printStr(" init-expr:{");
                print(x.filtExpr);
                printStr(" }");
            }

            if (x.steps.length == 0) {
                printStr(" steps {none}");
                printStr("}");
            } else {
                printStr(" steps {{");
                for (int i = 0; i < x.steps.length; i++) {
                    print(x.steps[i]);
                    if (i < x.steps.length - 1)
                        printStr(" } {");
                }
                printStr("}}}");
            }
        } else if (o instanceof XPathFilterExpr) {
            XPathFilterExpr x = (XPathFilterExpr)o;

            printStr("filter-expr:{{");
            print(x.x);

            if (x.predicates.length == 0) {
                printStr(" } predicates {none}}");
            } else {
                printStr(" } predicates {{");
                for (int i = 0; i < x.predicates.length; i++) {
                    print(x.predicates[i]);
                    if (i < x.predicates.length - 1)
                        printStr(" } {");
                }
                printStr(" }}}");
            }
        } else if (o instanceof XPathStep) {
            XPathStep x = (XPathStep)o;

            String axis = XPathStep.axisStr(x.axis);
            String test = x.testStr();

            if (x.predicates.length == 0) {
                printStr("step {axis:" + axis + " test:" + test + " predicates {none}}");
            } else {
                printStr("step {axis:" + axis + " test:" + test + " predicates {{");
                for (int i = 0; i < x.predicates.length; i++) {
                    print(x.predicates[i]);
                    if (i < x.predicates.length - 1)
                        printStr(" } {");
                }
                printStr("}}}");
            }
        }

        indent -= 1;
    }

    // Make sure hashCode and equals are implemented by child classes.
    // If you override one, it is best practice to also override the other.
    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object o);

    /**
     * @return a best-effort for the cannonical representation
     * of this expression. May not be one-to-one with the original
     * text, and may not be semantically complete, but should ideally
     * provide a human with a clear depiction of the expression.
     */
    public abstract String toPrettyString();

}
