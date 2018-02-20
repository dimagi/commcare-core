package org.javarosa.core.reference;


/**
 * Created by willpride on 2/20/18.
 */
public class ReferenceHandler {

    private static ReferenceManager staticManager;

    private static final ThreadLocal<ReferenceManager> threadLocalManager = new ThreadLocal<ReferenceManager>(){
        @Override
        protected ReferenceManager initialValue()
        {
            return new ReferenceManager();
        }
    };

    private static boolean useThreadLocal = false;

    public static void init(boolean force) {
        if (useThreadLocal) {
            if (threadLocalManager.get() == null || force) {
                threadLocalManager.set(new ReferenceManager());
            }
        } else {
            if (staticManager == null || force) {
                staticManager = new ReferenceManager();
            }
        }
    }

    public static void setUseThreadLocalStrategy(boolean useThreadLocal) {
        ReferenceHandler.useThreadLocal = useThreadLocal;
    }

    /**
     * @return Singleton accessor to the global
     * ReferenceManager.
     */
    public static ReferenceManager instance() {
        if (useThreadLocal) {
            return threadLocalManager.get();
        } else {
            return staticManager;
        }
    }
}
