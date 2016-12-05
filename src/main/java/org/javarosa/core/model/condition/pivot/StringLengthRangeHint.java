/**
 *
 */
package org.javarosa.core.model.condition.pivot;

import org.javarosa.core.model.data.StringData;

/**
 * @author ctsims
 */
public class StringLengthRangeHint extends RangeHint<StringData> {

    @Override
    protected StringData castToValue(double value) throws UnpivotableExpressionException {
        if (value > 50) {
            throw new UnpivotableExpressionException("No calculating string length pivots over 50 characters currently");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ((int)value); ++i) {
            sb.append("X");
        }
        return new StringData(sb.toString());
    }

    @Override
    protected double unit() {
        return 1;
    }

}
