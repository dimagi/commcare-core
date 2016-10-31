package org.javarosa.xpath.parser.ast;

import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathNumericLiteral;
import org.javarosa.xpath.expr.XPathQName;
import org.javarosa.xpath.expr.XPathStringLiteral;
import org.javarosa.xpath.expr.XPathVariableReference;
import org.javarosa.xpath.parser.Parser;
import org.javarosa.xpath.parser.Token;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Vector;

public class ASTNodeAbstractExpr extends ASTNode {
    public static final int CHILD = 1;
    public static final int TOKEN = 2;

    // mixture of tokens and ASTNodes
    public final Vector<Object> content;

    public ASTNodeAbstractExpr() {
        content = new Vector<>();
    }

    @Override
    public Vector getChildren() {
        Vector<Object> children = new Vector<>();
        for (int i = 0; i < content.size(); i++) {
            if (getType(i) == CHILD) {
                children.add(content.get(i));
            }
        }
        return children;
    }

    @Override
    public XPathExpression build() throws XPathSyntaxException {
        if (content.size() == 1) {
            if (getType(0) == CHILD) {
                return ((ASTNode)content.get(0)).build();
            } else {
                switch (getTokenType(0)) {
                    case Token.NUM:
                        return new XPathNumericLiteral((Double)getToken(0).val);
                    case Token.STR:
                        return new XPathStringLiteral((String)getToken(0).val);
                    case Token.VAR:
                        return new XPathVariableReference((XPathQName)getToken(0).val);
                    default:
                        throw new XPathSyntaxException();
                }
            }
        } else {
            throw new XPathSyntaxException();
        }
    }

    private boolean isTerminal() {
        if (content.size() == 1) {
            int type = getTokenType(0);
            return (type == Token.NUM || type == Token.STR || type == Token.VAR);
        } else {
            return false;
        }
    }

    public boolean isNormalized() {
        if (content.size() == 1 && getType(0) == CHILD) {
            ASTNode child = (ASTNode)content.get(0);
            if (child instanceof ASTNodePathStep || child instanceof ASTNodePredicate) {
                throw new RuntimeException("shouldn't happen");
            }
            return true;
        } else {
            return isTerminal();
        }
    }

    public int getType(int i) {
        Object o = content.get(i);
        if (o instanceof Token)
            return TOKEN;
        else if (o instanceof ASTNode)
            return CHILD;
        else
            return -1;
    }

    public Token getToken(int i) {
        return (getType(i) == TOKEN ? (Token)content.get(i) : null);
    }

    public int getTokenType(int i) {
        Token t = getToken(i);
        return (t == null ? -1 : t.type);
    }

    //create new node containing children from [start,end)
    public ASTNodeAbstractExpr extract(int start, int end) {
        ASTNodeAbstractExpr node = new ASTNodeAbstractExpr();
        for (int i = start; i < end; i++) {
            node.content.add(content.get(i));
        }
        return node;
    }

    /**
     * remove children from [start,end) and replace with node n
     */
    public void condense(ASTNode node, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            content.remove(i);
        }
        content.add(start, node);
    }

    /**
     * Replace contents (which should be just tokens) with a single node
     */
    public void condenseFull(ASTNode node) {
        content.clear();
        content.add(node);
    }

    //find the next incidence of 'target' at the current stack level
    //start points to the opening of the current stack level
    public int indexOfBalanced(int start, int target, int leftPush, int rightPop) {
        int depth = 0;
        int i = start + 1;
        boolean found = false;

        while (depth >= 0 && i < content.size()) {
            int type = getTokenType(i);

            if (depth == 0 && type == target) {
                found = true;
                break;
            }

            if (type == leftPush)
                depth++;
            else if (type == rightPop)
                depth--;

            i++;
        }

        return (found ? i : -1);
    }

    public static class Partition {
        public final Vector<ASTNodeAbstractExpr> pieces;
        public final Vector<Integer> separators;

        public Partition() {
            pieces = new Vector<>();
            separators = new Vector<>();
        }
    }

    //paritition the range [start,end), separating by any occurrence of separator
    public Partition partition(int[] separators, int start, int end) {
        Partition part = new Partition();
        Vector<Integer> sepIdxs = new Vector<>();

        for (int i = start; i < end; i++) {
            for (int separator : separators) {
                if (getTokenType(i) == separator) {
                    part.separators.addElement(separator);
                    sepIdxs.addElement(i);
                    break;
                }
            }
        }

        for (int i = 0; i <= sepIdxs.size(); i++) {
            int pieceStart = (i == 0 ? start : Parser.vectInt(sepIdxs, i - 1) + 1);
            int pieceEnd = (i == sepIdxs.size() ? end : Parser.vectInt(sepIdxs, i));
            part.pieces.addElement(extract(pieceStart, pieceEnd));
        }

        return part;
    }

    //partition by sep, to the end of the current stack level
    //start is the opening token of the current stack level
    public Partition partitionBalanced(int sep, int start, int leftPush, int rightPop) {
        Partition part = new Partition();
        Vector<Integer> sepIdxs = new Vector<>();
        int end = indexOfBalanced(start, rightPop, leftPush, rightPop);
        if (end == -1) {
            return null;
        }

        int k = start;
        do {
            k = indexOfBalanced(k, sep, leftPush, rightPop);
            if (k != -1) {
                sepIdxs.addElement(k);
                part.separators.addElement(sep);
            }
        } while (k != -1);

        for (int i = 0; i <= sepIdxs.size(); i++) {
            int pieceStart = (i == 0 ? start + 1 : Parser.vectInt(sepIdxs, i - 1) + 1);
            int pieceEnd = (i == sepIdxs.size() ? end : Parser.vectInt(sepIdxs, i));
            part.pieces.addElement(extract(pieceStart, pieceEnd));
        }

        return part;
    }

    public int size() {
        return content.size();
    }

    /**
     * true if 'node' is potentially a step, as opposed to a filter expr
     */
    public boolean isStep() {
        if (content.size() > 0) {
            int type = getTokenType(0);
            if (type == Token.QNAME ||
                    type == Token.WILDCARD ||
                    type == Token.NSWILDCARD ||
                    type == Token.AT ||
                    type == Token.DOT ||
                    type == Token.DBL_DOT) {
                return true;
            } else if (content.get(0) instanceof ASTNodeFunctionCall) {
                String name = ((ASTNodeFunctionCall)content.get(0)).name.toString();
                return (name.equals("node") || name.equals("text") || name.equals("comment") || name.equals("processing-instruction"));
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
