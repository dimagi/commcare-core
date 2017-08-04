package org.commcare.util.screen;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.trace.AccumulatingReporter;
import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.core.model.trace.EvaluationTraceReporter;
import org.javarosa.core.model.trace.StringEvaluationTraceSerializer;

import java.util.Vector;

/**
 * Generally useful methods on CLI screens.
 *
 * Created by ctsims on 8/20/2015.
 */
public class ScreenUtils {

    public static void addPaddedStringToBuilder(StringBuilder builder, String s, int width) {
        if (s.length() > width) {
            builder.append(s, 0, width);
            return;
        }
        builder.append(s);
        if (s.length() != width) {
            // add whitespace padding
            for (int i = 0; i < width - s.length(); ++i) {
                builder.append(' ');
            }
        }
    }

    public static String pad(String s, int width) {
        StringBuilder builder = new StringBuilder();
        addPaddedStringToBuilder(builder, s, width);
        return builder.toString();
    }

    public static class HereDummyFunc implements IFunctionHandler {
        private final double lat;
        private final double lon;

        public HereDummyFunc(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public String getName() {
            return "here";
        }

        @Override
        public Vector getPrototypes() {
            Vector<Class[]> p = new Vector<>();
            p.addElement(new Class[0]);
            return p;
        }

        @Override
        public boolean rawArgs() {
            return false;
        }

        @Override
        public Object eval(Object[] args, EvaluationContext ec) {
            return new GeoPointData(new double[]{lat, lon, 0, 10}).getDisplayText();
        }
    }
}
