package org.javarosa.j2me.util;

import org.javarosa.core.api.State;
import org.javarosa.core.log.WrappedException;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.log.HandledThread;
import org.javarosa.j2me.util.media.ImageUtils;
import org.javarosa.j2me.view.J2MEDisplay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.StringItem;

import de.enough.polish.ui.FramedForm;
import de.enough.polish.ui.ImageItem;

public abstract class GPRSTestState implements State, TrivialTransitions, HandledCommandListener {

    static final String DEFAULT_URL = "https://www.google.com";

    String url;

    Vector<String> messages;

    FramedForm interactiveView;
    ImageItem imageView;
    StringItem interactiveMessage;

    Command exit;
    Command back;
    Command details;
    Date start = null;

    public GPRSTestState () {
        this(DEFAULT_URL);
    }

    public GPRSTestState (String url) {
        this.url = url;
    }

    public void start () {
        messages = new Vector<String>();

        //#style networkTestForm
        interactiveView = new FramedForm(Localization.get("network.test.title"));
        exit = new Command(Localization.get("polish.command.ok"), Command.BACK, 0);
        back = new Command(Localization.get("polish.command.back"), Command.BACK, 0);
        details = new Command(Localization.get("network.test.details"), Command.SCREEN, 2);
        interactiveView.setCommandListener(this);
        interactiveView.addCommand(exit);

        //#style networkTestImage
        imageView = new ImageItem(null, null, ImageItem.LAYOUT_CENTER | ImageItem.LAYOUT_VCENTER, "");

        interactiveMessage = new StringItem(null, "");

        interactiveView.append(Graphics.TOP, imageView);
        interactiveView.append(interactiveMessage);

        J2MEDisplay.setView(interactiveView);

        final GPRSTestState parent = this;
        new HandledThread () {
            public void _run () {
                networkTest(parent);
            }
        }.start();
    }


    private void showDetails() {
        Form details = new Form(Localization.get("network.test.details.title"));
        for(String s: messages) {
            details.append(s);
        }
        details.addCommand(back);
        details.setCommandListener(this);
        J2MEDisplay.setView(details);
    }

    public void updateInfo (String keyRoot) {
        updateInfo(keyRoot, "");
    }
    public void updateInfo (String keyRoot, String details) {
        String message = Localization.get(keyRoot + ".message");
        Image image = null;
        try {
            String imageRoot = Localization.get(keyRoot + ".image");
            image = ImageUtils.getImage(imageRoot);
        } catch(NoLocalizedTextException nlte) {
            //no image for this one. No worries
        }

        Date now = new Date();
        if (start == null) {
            start = now;
        }

        int diff = (int)(now.getTime() - start.getTime()) / 10;
        String sDiff = (diff / 100) + "." + DateUtils.intPad(diff % 100, 2);



        interactiveMessage.setText(message);
        messages.addElement(sDiff + ": " + message + "\n" + details);
        if(image != null) {
            int[] newScales = ImageUtils.getNewDimensions(image, imageView.itemHeight, imageView.itemWidth);

            imageView.setImage(ImageUtils.resizeImage(image, newScales[1], newScales[0]));
        }
    }

    public static void networkTest (GPRSTestState parent) {
        HttpConnection conn = null;
        InputStream is = null;

        try {
            parent.updateInfo("network.test.begin", "URL: " + parent.url);

            conn = (HttpConnection)Connector.open(parent.url);
            conn.setRequestMethod(HttpConnection.GET);

            parent.updateInfo("network.test.connecting");

            int code = conn.getResponseCode();

            parent.updateInfo("network.test.connected",
                    "Response Code: " + code + "\nType: " +  conn.getType() + "\nLength: " + conn.getLength());

            byte[] data;
            is = conn.openInputStream();

            int len = (int)conn.getLength();
            if (len > 0) {
                int actual = 0;
                int bytesread = 0;
                data = new byte[len];
                while ((bytesread != len) && (actual != -1)) {
                    actual = is.read(data, bytesread, len - bytesread);
                    bytesread += actual;
                }
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int ch;
                while ((ch = is.read()) != -1) {
                     baos.write(ch);
                }
                data = baos.toByteArray();
            }
            parent.updateInfo("network.test.response", "Response length: " + data.length);
            String body;
            try {
                String encoding = conn.getEncoding();
                   body = new String(data, encoding != null ? encoding : "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < data.length; i++)
                    sb.append((char)data[i]);
                body = sb.toString();
            }
            parent.updateInfo("network.test.content", "Response Body: " + body);

        } catch (Exception e) {
            parent.updateInfo("network.test.failed", "Error: " + WrappedException.printException(e));
        } finally {
            try {
                if (is != null)
                    is.close();
                if (conn != null)
                    conn.close();
            } catch (IOException ioe) { }
        }
        parent.interactiveView.addCommand(parent.details);
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command c, Displayable d) {
        if (c == exit) {
            done();
        } else if(c == details) {
            showDetails();
        } else if(c == back) {
            J2MEDisplay.setView(interactiveView);
        }
    }


    //nokia s40 bug
    public abstract void done();
}
