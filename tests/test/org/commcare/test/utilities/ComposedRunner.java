package org.commcare.test.utilities;

import org.junit.runners.ParentRunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for composing multiple test runners. Used in conjunction with RunnerCoupler
 *
 * @author ctsims
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ComposedRunner {
    Class<? extends ParentRunner>[] runners();
    Class[] runnables();
}
