package org.commcare.test.utilities;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A JUnit runner that permits using multiple compatible runners in the same test file.
 *
 * The runners themselves need to be provided with the ComposedRunner annotation as ordered lists
 * which contain the different runners and the type of child objects that they are running.
 *
 * Runners can only be composed if they have different child test class types.
 *
 * Created by ctsims on 10/13/2015.
 */
public class RunnerCoupler extends ParentRunner<Object> {

    private final HashMap<Class, ParentRunner> runners = new HashMap<>();

    public RunnerCoupler(Class<?> clazz) throws InitializationError {
        super(clazz);
        try {
            for (Annotation a : clazz.getAnnotations()) {
                if (a instanceof ComposedRunner) {
                    ComposedRunner runner = ((ComposedRunner)a);

                    for (int i = 0; i < runner.runnables().length; ++i) {
                        runners.put(runner.runnables()[i],
                                (runner.runners()[i]).getConstructor(Class.class).newInstance(clazz));
                    }
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException
                | InstantiationException | IllegalAccessException sme) {
            throw new RuntimeException("Invalid Composed runner. " +
                    "All composable runners must have a public constructor that accepts a class");
        }
    }

    // Note: All of these composition methods have to run via reflection because they are at a
    // protected level. It's awkward, but functional

    @Override
    protected List<Object> getChildren() {
        List<Object> tests = new ArrayList<>();
        for (ParentRunner<?> runner : runners.values()) {
            try {
                Class<? extends ParentRunner> c = runner.getClass();
                Method m = c.getDeclaredMethod("getChildren");
                m.setAccessible(true);
                tests.addAll((List<Object>)m.invoke(runner));
            } catch (InvocationTargetException e) {
                RuntimeException re = new RuntimeException("Error loading tests");
                re.initCause(e.getCause());
                throw re;
            } catch (NoSuchMethodException | IllegalAccessException e) {
                RuntimeException re =
                        new RuntimeException("Illegal access exception running tests");
                re.initCause(e);
                throw re;
            }
        }
        return tests;
    }

    @Override
    protected Description describeChild(Object child) {
        ParentRunner runner = runners.get(child.getClass());
        try {
            Class<? extends ParentRunner> c = runner.getClass();
            Method m = c.getDeclaredMethod("describeChild", child.getClass());
            m.setAccessible(true);
            return (Description)m.invoke(runner, child);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            RuntimeException re = new RuntimeException("Illegal access exception running tests");
            re.initCause(e);
            throw re;
        }
    }

    @Override
    protected void runChild(Object child, RunNotifier notifier) {
        ParentRunner runner = runners.get(child.getClass());
        try {
            Class<? extends ParentRunner> c = runner.getClass();
            Method m = c.getDeclaredMethod("runChild", child.getClass(), RunNotifier.class);
            m.setAccessible(true);
            m.invoke(runner, child, notifier);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            RuntimeException re = new RuntimeException("Illegal access exception running tests");
            re.initCause(e);
            throw re;
        }
    }
}