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

    public static void setUseThreadLocalStrategy(boolean useThreadLocal) {
        ReferenceHandler.useThreadLocal = useThreadLocal;
    }

    /**
     * @return Singleton accessor to the global
     * ReferenceManager.
     */
    public static ReferenceManager instance() {
        if (useThreadLocal) {
            if (threadLocalManager.get() == null) {
                threadLocalManager.set(new ReferenceManager());
            }
            return threadLocalManager.get();
        } else {
            if (staticManager == null) {
                staticManager = new ReferenceManager();
            }
            return staticManager;
        }
    }
}
