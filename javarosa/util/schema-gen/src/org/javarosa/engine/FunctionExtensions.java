package org.javarosa.engine;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;

import java.util.Date;
import java.util.Vector;

/**
 * Custom functions to be used in form player debug tools.
 *
 * Allows users to:
 *  - override 'today()' and 'now()' with custom dates.
 *  - perform introspection on xpath references with 'print()'
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
class FunctionExtensions {
    protected static class TodayFunc implements IFunctionHandler {
        private final String name;
        private final Date date;

        public TodayFunc(String name, Date date) {
            this.name = name;
            this.date = date;
        }

        @Override
        public String getName() {
            return name;
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
            if (date != null) {
                return date;
            } else {
                return new Date();
            }
        }
    }
}
