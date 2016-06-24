package org.javarosa.j2me.log.viewer;

import org.javarosa.core.api.State;
import org.javarosa.core.log.LogEntry;
import org.javarosa.core.log.StreamLogSerializer;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.Logger;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;

import java.io.IOException;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

public abstract class LogViewerState implements State, TrivialTransitions, HandledCommandListener {

    static final int DEFAULT_MAX_ENTRIES = 200;

    int max_entries;
    Command exit;
    Command submit;
    Command ok;

    protected Form view;
    Thread submitThread;

    public LogViewerState () {
        this(DEFAULT_MAX_ENTRIES);
    }

    public LogViewerState (int max_entries) {
        this.max_entries = max_entries;
    }

    public void start () {
        view = new Form("logs");

        view.setCommandListener(this);

        exit = new Command("OK", Command.BACK, 1);

        submit = new Command("Send to Server", Command.SCREEN, 0);

        ok = new Command("OK", Command.SCREEN, 0);

        loadLogs();

        J2MEDisplay.setView(view);
    }

    private void loadLogs() {
        view.deleteAll();
        this.view.removeCommand(ok);
        try {
            //TODO: Start from other end of logs
            Logger._().serializeLogs(new StreamLogSerializer() {

                String prevDateStr;

                protected void serializeLog(LogEntry entry) throws IOException {
                    String fullDateStr = DateUtils.formatDateTime(entry.getTime(), DateUtils.FORMAT_ISO8601).substring(0, 19);
                    String dateStr = fullDateStr.substring(11);
                    if (prevDateStr == null || !fullDateStr.substring(0, 10).equals(prevDateStr.substring(0, 10))) {
                        view.append("= " + fullDateStr.substring(0, 10) + " =");
                    }
                    prevDateStr = fullDateStr;

                    String line = dateStr + ":" + entry.getType() + "> " + entry.getMessage();

                    view.append(new StringItem("", line));

                }
            }, max_entries);

        } catch (IOException e) {
            view.append(new StringItem("", "Error reading logs..."));
        }

        int count = Logger._().logSize();
        if(count > max_entries) {
            view.append("..." + count + " More");
        }
        addCmd();
    }

    private void addCmd() {
        if(submitSupported()) {
            view.addCommand(exit);
            view.addCommand(submit);
        }
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command c, Displayable d) {
        if (c == exit) {
            done();
        } else if (c == submit) {
            Logger.log("maintenance", "Manual Log Sending Triggered");
            if(submitSupported()) {
                submitThread = new Thread(new Runnable() {

                    public void run() {
                        try {
                            submit();
                            view.addCommand(ok);
                        } catch(Exception e) {
                            e.printStackTrace();
                            append("Log Submission Failed: " + e.getMessage(), false);
                            addCmd();
                        }
                    }
                });

                submitThread.start();

                this.view.removeCommand(exit);
                this.view.removeCommand(submit);
            } else {
                append("Log Submission not Supported on this Platform", true);
            }
        } else if (c == ok) {
            loadLogs();
        }

    }

    //nokia s40 bug
    public abstract void done ();

    public boolean submitSupported() {
        return false;
    }

    public void submit() {
    }

    public void append(String message, boolean clear) {
        if(clear){
            view.deleteAll();
        }
        view.append(new StringItem("", message));
    }
}
