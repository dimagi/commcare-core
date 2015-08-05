package org.javarosa.j2me.log;

import org.javarosa.core.services.Logger;
import org.javarosa.j2me.util.CommCareHandledExceptionState;
import org.javarosa.j2me.view.J2MEDisplay;

import java.lang.ref.WeakReference;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Item;

/**
 * This class provides exception-handling wrappers for the GUI event interfaces CommandListener,
 * ItemStateListener, etc.
 *
 * The entry point of the interface (i.e, CommandListener.commandAction()) calls out to this class,
 * which sets up an exception trap, then delegates back to the calling class via a helper method.
 *
 * @author Drew Roos
 *
 */
public class CrashHandler {

    private static final Object lock = new Object();
    private static WeakReference exceptionStateHolder;
    private static WeakReference expired;

    /**
     * other places exceptions need to be explicitly trapped
     *
     * 1) anywhere that app code is called from a background thread or timeout (SplashScreen,
     *    network callbacks (if any?))
     * 2) anywhere a GUI event handler can trigger app code; most of these are handled by the
     *    wrappers here, but there are still places such as key*(), pointer*(), (handle)Key*(),
     *    and (handle)Pointer*()
     * 3) anywhere the app code launches a Runnable (thread or timertask)
     *
     */

    public static void commandAction(HandledCommandListener handler, Command c, Displayable d) {
        synchronized(lock) {
            if(expired != null) {
                Displayable old = (Displayable)expired.get();
                if(old == null) { expired = null; }
                if(d == old) {
                    return;
                }
            }
        }
        try {
            handler._commandAction(c, d);
        } catch (Exception e) {
            tryCCHES("gui-cl", e);
        }
    }

    public static void commandAction(HandledPCommandListener handler, de.enough.polish.ui.Command c, de.enough.polish.ui.Displayable d) {
        synchronized(lock) {
            if(expired != null) {
                Displayable old = (Displayable)expired.get();
                if(old == null) { expired = null; }
                if(d == old) {
                    return;
                }
            }
        }
        try {
            handler._commandAction(c, d);
        } catch (Exception e) {
            tryCCHES("gui-clp", e);
        }
    }

    public static void commandAction(HandledItemCommandListener handler, Command c, Item i) {
        try {
            handler._commandAction(c, i);
        } catch (Exception e) {
            tryCCHES("gui-icl", e);
        }
    }

    public static void commandAction(HandledPItemCommandListener handler,
            de.enough.polish.ui.Command c, de.enough.polish.ui.Item i) {
        try {
            handler._commandAction(c, i);
        } catch (Exception e) {
            tryCCHES("gui-iclp", e);
        }
    }

    public static void itemStateChanged(HandledItemStateListener handler, Item i) {
        try {
            handler._itemStateChanged(i);
        } catch (Exception e) {
            tryCCHES("gui-islp", e);
        }
    }

    public static void itemStateChanged(HandledPItemStateListener handler, de.enough.polish.ui.Item i) {
        try {
            handler._itemStateChanged(i);
        } catch (Exception e) {
            tryCCHES("gui-islp", e);
        }
    }

    public static void executeHandledThread(HandledThread thread){
        try{
            thread._run();
        } catch(Exception e){
            tryCCHES("gui-islt", e);
        }
    }

    public static void executeWrappedRunnable(Runnable r){
        try{r.run();}
        catch(Exception e){
            tryCCHES("gui-isrw", e);
        }
    }

    public static void tryCCHES(String failString, Exception e){
        expired = null;

        if(exceptionStateHolder == null){Logger.die(failString, e);}

        CommCareHandledExceptionState cches = (CommCareHandledExceptionState)exceptionStateHolder.get();

        if(cches == null){Logger.die(failString, e);}
        if(cches.handlesException(e)){
            cches.setErrorMessage(e.getMessage());
            J2MEDisplay.startStateWithLoadingScreen(cches);
        }
        else{
            Logger.die(failString, e);
        }
    }

    public static void setExceptionHandler(CommCareHandledExceptionState s){
        exceptionStateHolder = new WeakReference(s);
    }

    public static void expire(Displayable d) {
        synchronized(lock) {
            expired = new WeakReference(d);
        }
    }
}
