package org.javarosa.core.services.locale;

/**
 * (Yet another) Manager class, this one for determining which localization strategy to use.
 * The options are:
 *
 *  1. staticLocalizer: Static variable for platforms where the same Localizer can be safely shared across
 *     all threads on the JVM (Android)
 *  2. threadLocalLocalizer: ThreadLocal variable for platforms where different threads are potentially
 *     running separate applications (Web Apps)
 *
 *  Defaults to the static Localizer. Web Apps should set the strategy to useThreadLocal = true immediately
 *  on startup.
 *
 *  @author wpride
 */
public class LocalizerManager {

    private static Localizer staticLocalizer;

    private static final ThreadLocal<Localizer> threadLocalLocalizer = new ThreadLocal<Localizer>(){
        @Override
        protected Localizer initialValue()
        {
            return new Localizer(true, false);
        }
    };

    private static boolean useThreadLocal = false;

    public static Localizer getGlobalLocalizer() {
        if (useThreadLocal) {
            return threadLocalLocalizer.get();
        } else {
            return staticLocalizer;
        }
    }

    public static void init(boolean force) {
        if (useThreadLocal) {
            if (threadLocalLocalizer.get() == null || force) {
                threadLocalLocalizer.set(new Localizer(true, false));
            }
        } else {
            if (staticLocalizer == null || force) {
                staticLocalizer = new Localizer(true, false);
            }
        }
    }

    public static void setUseThreadLocalStrategy(boolean useThreadLocal) {
        LocalizerManager.useThreadLocal = useThreadLocal;
    }
}
