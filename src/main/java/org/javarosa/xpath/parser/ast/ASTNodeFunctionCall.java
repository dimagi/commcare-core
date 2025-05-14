package org.javarosa.xpath.parser.ast;

import org.javarosa.xpath.expr.XPathAbsFunc;
import org.javarosa.xpath.expr.XPathAcosFunc;
import org.javarosa.xpath.expr.XPathAsinFunc;
import org.javarosa.xpath.expr.XPathAtanFunc;
import org.javarosa.xpath.expr.XPathAtanTwoFunc;
import org.javarosa.xpath.expr.XPathBooleanFromStringFunc;
import org.javarosa.xpath.expr.XPathBooleanFunc;
import org.javarosa.xpath.expr.XPathClosestPointToPolygonFunc;
import org.javarosa.xpath.expr.XPathCeilingFunc;
import org.javarosa.xpath.expr.XPathChecklistFunc;
import org.javarosa.xpath.expr.XPathChecksumFunc;
import org.javarosa.xpath.expr.XPathConcatFunc;
import org.javarosa.xpath.expr.XPathCondFunc;
import org.javarosa.xpath.expr.XPathContainsFunc;
import org.javarosa.xpath.expr.XPathCosFunc;
import org.javarosa.xpath.expr.XPathCountFunc;
import org.javarosa.xpath.expr.XPathCountSelectedFunc;
import org.javarosa.xpath.expr.XPathCustomRuntimeFunc;
import org.javarosa.xpath.expr.XPathDateFunc;
import org.javarosa.xpath.expr.XPathDecryptStringFunc;
import org.javarosa.xpath.expr.XPathDependFunc;
import org.javarosa.xpath.expr.XPathDistanceFunc;
import org.javarosa.xpath.expr.XPathDistinctValuesFunc;
import org.javarosa.xpath.expr.XPathDoubleFunc;
import org.javarosa.xpath.expr.XPathEncryptStringFunc;
import org.javarosa.xpath.expr.XPathEndsWithFunc;
import org.javarosa.xpath.expr.XPathExpFunc;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFalseFunc;
import org.javarosa.xpath.expr.XPathFloorFunc;
import org.javarosa.xpath.expr.XPathFormatDateForCalendarFunc;
import org.javarosa.xpath.expr.XPathFormatDateFunc;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathIdCompressFunc;
import org.javarosa.xpath.expr.XPathIfFunc;
import org.javarosa.xpath.expr.XPathIndexOfFunc;
import org.javarosa.xpath.expr.XPathIntFunc;
import org.javarosa.xpath.expr.XPathJoinChunkFunc;
import org.javarosa.xpath.expr.XPathJoinFunc;
import org.javarosa.xpath.expr.XPathJsonPropertyFunc;
import org.javarosa.xpath.expr.XPathLogFunc;
import org.javarosa.xpath.expr.XPathLogTenFunc;
import org.javarosa.xpath.expr.XPathLowerCaseFunc;
import org.javarosa.xpath.expr.XPathMaxFunc;
import org.javarosa.xpath.expr.XPathMinFunc;
import org.javarosa.xpath.expr.XPathNotFunc;
import org.javarosa.xpath.expr.XPathNowFunc;
import org.javarosa.xpath.expr.XPathNumberFunc;
import org.javarosa.xpath.expr.XPathPiFunc;
import org.javarosa.xpath.expr.XPathIsPointInsidePolygonFunc;
import org.javarosa.xpath.expr.XPathPositionFunc;
import org.javarosa.xpath.expr.XPathPowFunc;
import org.javarosa.xpath.expr.XPathQName;
import org.javarosa.xpath.expr.XPathRandomFunc;
import org.javarosa.xpath.expr.XPathRegexFunc;
import org.javarosa.xpath.expr.XPathReplaceFunc;
import org.javarosa.xpath.expr.XPathRoundFunc;
import org.javarosa.xpath.expr.XPathSelectedAtFunc;
import org.javarosa.xpath.expr.XPathSelectedFunc;
import org.javarosa.xpath.expr.XPathSinFunc;
import org.javarosa.xpath.expr.XPathSleepFunc;
import org.javarosa.xpath.expr.XPathSortByFunc;
import org.javarosa.xpath.expr.XPathSortFunc;
import org.javarosa.xpath.expr.XPathSqrtFunc;
import org.javarosa.xpath.expr.XPathStartsWithFunc;
import org.javarosa.xpath.expr.XPathStringFunc;
import org.javarosa.xpath.expr.XPathStringLengthFunc;
import org.javarosa.xpath.expr.XPathSubstrFunc;
import org.javarosa.xpath.expr.XPathSubstringAfterFunc;
import org.javarosa.xpath.expr.XPathSubstringBeforeFunc;
import org.javarosa.xpath.expr.XPathSumFunc;
import org.javarosa.xpath.expr.XPathTanFunc;
import org.javarosa.xpath.expr.XPathTodayFunc;
import org.javarosa.xpath.expr.XPathTranslateFunc;
import org.javarosa.xpath.expr.XPathTrueFunc;
import org.javarosa.xpath.expr.XPathUpperCaseFunc;
import org.javarosa.xpath.expr.XPathUuidFunc;
import org.javarosa.xpath.expr.XPathWeightedChecklistFunc;
import org.javarosa.xpath.expr.XpathCoalesceFunc;
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
            case "checksum":
                return new XPathChecksumFunc(args);
            case "sort":
                return new XPathSortFunc(args);
            case "sort-by":
                return new XPathSortByFunc(args);
            case "distinct-values":
                return new XPathDistinctValuesFunc(args);
            case "sleep":
                return new XPathSleepFunc(args);
            case "index-of":
                return new XPathIndexOfFunc(args);
            case "encrypt-string":
                return new XPathEncryptStringFunc(args);
            case "decrypt-string":
                return new XPathDecryptStringFunc(args);
            case "json-property":
                return new XPathJsonPropertyFunc(args);
            case "closest-point-on-polygon":
                return new XPathClosestPointToPolygonFunc(args);
            case "is-point-inside-polygon":
                return new XPathIsPointInsidePolygonFunc(args);
            default:
                return new XPathCustomRuntimeFunc(name, args);
        }
    }
}
