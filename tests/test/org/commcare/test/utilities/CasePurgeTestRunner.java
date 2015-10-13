package org.commcare.test.utilities;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ctsims on 10/13/2015.
 */
public class CasePurgeTestRunner extends ParentRunner<CasePurgeTest> {

    Class<?> testClass;

    public CasePurgeTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        this.testClass = clazz;
    }

    @Override
    protected List<CasePurgeTest> getChildren() {
        List<CasePurgeTest> tests = new ArrayList<>();
        for(Method m : testClass.getMethods()) {
            RunWithResource r = m.getAnnotation(RunWithResource.class);
            if(r != null) {
                for(CasePurgeTest t : CasePurgeTest.getTests(r.value())) {
                    tests.add(t);
                }
            }
        }
        return tests;
    }

    @Override
    protected Description describeChild(CasePurgeTest child) {
        return Description.createTestDescription(CasePurgeTest.class, child.getName());
    }

    @Override
    protected void runChild(CasePurgeTest child, RunNotifier notifier) {
        try {
            notifier.fireTestStarted(describeChild(child));
            child.executeTest();
            notifier.fireTestFinished(describeChild(child));
        } catch(Throwable throwable ){
            notifier.fireTestFailure(new Failure(describeChild(child), throwable));
        }
    }
}