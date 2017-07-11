package org.javarosa.xpath.parser.ast;

import org.javarosa.xpath.expr.*;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.List;

public class ASTNodeFunctionCall extends ASTNode {
    public final XPathQName name;
    public List<? extends ASTNode> args;

    public ASTNodeFunctionCall(XPathQName name) {
        this.name = name;
        args = new ArrayList<>();
    }

    @Override
    public List<? extends ASTNode> getChildren() {
        return args;
    }

    @Override
    public XPathExpression build() throws XPathSyntaxException {
        XPathExpression[] xargs = new XPathExpression[args.size()];
        for (int i = 0; i < args.size(); i++) {
            xargs[i] = args.get(i).build();
        }

        return buildFuncExpr(name.toString(), xargs);
    }

    private static XPathFuncExpr buildFuncExpr(String name, XPathExpression[] args)
            throws XPathSyntaxException {
        switch (name) {
            case "if":
                return new XPathIfFunc(args);
            case "coalesce":
                return new XpathCoalesceFunc(args);
            case "cond":
                return new XPathCondFunc(args);
            case "true":
                return new XPathTrueFunc(args);
            case "false":
                return new XPathFalseFunc(args);
            case "boolean":
                return new XPathBooleanFunc(args);
            case "number":
                return new XPathNumberFunc(args);
            case "int":
                return new XPathIntFunc(args);
            case "double":
                return new XPathDoubleFunc(args);
            case "string":
                return new XPathStringFunc(args);
            case "date":
                return new XPathDateFunc(args);
            case "not":
                return new XPathNotFunc(args);
            case "boolean-from-string":
                return new XPathBooleanFromStringFunc(args);
            case "format-date":
                return new XPathFormatDateFunc(args);
            case "selected":
                // fall-through to is-selected on purpose
            case "is-selected":
                return new XPathSelectedFunc(name, args);
            case "count-selected":
                return new XPathCountSelectedFunc(args);
            case "selected-at":
                return new XPathSelectedAtFunc(args);
            case "position":
                return new XPathPositionFunc(args);
            case "count":
                return new XPathCountFunc(args);
            case "sum":
                return new XPathSumFunc(args);
            case "max":
                return new XPathMaxFunc(args);
            case "min":
                return new XPathMinFunc(args);
            case "today":
                return new XPathTodayFunc(args);
            case "now":
                return new XPathNowFunc(args);
            case "concat":
                return new XPathConcatFunc(args);
            case "join":
                return new XPathJoinFunc(args);
            case "substr":
                return new XPathSubstrFunc(args);
            case "substring-before":
                return new XPathSubstringBeforeFunc(args);
            case "substring-after":
                return new XPathSubstringAfterFunc(args);
            case "string-length":
                return new XPathStringLengthFunc(args);
            case "upper-case":
                return new XPathUpperCaseFunc(args);
            case "lower-case":
                return new XPathLowerCaseFunc(args);
            case "contains":
                return new XPathContainsFunc(args);
            case "starts-with":
                return new XPathStartsWithFunc(args);
            case "ends-with":
                return new XPathEndsWithFunc(args);
            case "translate":
                return new XPathTranslateFunc(args);
            case "replace":
                return new XPathReplaceFunc(args);
            case "checklist":
                return new XPathChecklistFunc(args);
            case "weighted-checklist":
                return new XPathWeightedChecklistFunc(args);
            case "regex":
                return new XPathRegexFunc(args);
            case "depend":
                return new XPathDependFunc(args);
            case "random":
                return new XPathRandomFunc(args);
            case "uuid":
                return new XPathUuidFunc(args);
            case "pow":
                return new XPathPowFunc(args);
            case "abs":
                return new XPathAbsFunc(args);
            case "ceiling":
                return new XPathCeilingFunc(args);
            case "floor":
                return new XPathFloorFunc(args);
            case "round":
                return new XPathRoundFunc(args);
            case "log":
                return new XPathLogFunc(args);
            case "log10":
                return new XPathLogTenFunc(args);
            case "sin":
                return new XPathSinFunc(args);
            case "cos":
                return new XPathCosFunc(args);
            case "tan":
                return new XPathTanFunc(args);
            case "asin":
                return new XPathAsinFunc(args);
            case "acos":
                return new XPathAcosFunc(args);
            case "atan":
                return new XPathAtanFunc(args);
            case "atan2":
                return new XPathAtanTwoFunc(args);
            case "sqrt":
                return new XPathSqrtFunc(args);
            case "exp":
                return new XPathExpFunc(args);
            case "pi":
                return new XPathPiFunc(args);
            case "distance":
                return new XPathDistanceFunc(args);
            case "format-date-for-calendar":
                return new XPathFormatDateForCalendarFunc(args);
            case "join-chunked":
                return new XPathJoinChunkFunc(args);
            case "id-compress":
                return new XPathIdCompressFunc(args);
            case "sort":
                return new XPathSortFunc(args);
            case "sort-by":
                return new XPathSortByFunc(args);
            default:
                return new XPathCustomRuntimeFunc(name, args);
        }
    }
}
