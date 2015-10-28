package org.commcare.api.xml;

import org.commcare.api.engine.XFormPlayer;
import org.commcare.api.screens.Screen;
import org.commcare.api.session.SessionUtils;
import org.commcare.api.session.SessionWrapper;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.parse.ParseUtils;
import org.commcare.api.session.CommCareSessionException;
import org.javarosa.core.model.User;
import org.javarosa.core.services.storage.IStorageIterator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by willpride on 10/27/15.
 */
public class XmlProcessor {

    SessionWrapper sessionWrapper;
    XFormPlayer xFormPlayer;

    public String processRespondXML(File xmlRequest) throws Exception{
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlRequest);
        NodeList nodeList = doc.getElementsByTagName("command");

        sessionWrapper = null;
        xFormPlayer = null;

        for(int i = 0; i < nodeList.getLength(); i++){
            Node node = nodeList.item(i);
            processCommandNode(node);
        }
        return "";
    }

    private void processCommandNode(Node node) throws Exception {
        String commandName = null;
        ArrayList<String> commandArgs = new ArrayList<>();
        for(int i = 0; i < node.getChildNodes().getLength(); i++){
            Node child = node.getChildNodes().item(i);
            switch(child.getNodeName()){
                case "name":
                    commandName = child.getTextContent();
                    break;
                case "arg":
                    commandArgs.add(child.getTextContent());
                    break;
            }
        }

        String[] returnArray = new String[commandArgs.size()];
        commandArgs.toArray(returnArray);

        handleCommandObject(commandName, returnArray);
    }

    private void handleCommandObject(String command, String[] args) throws Exception {
        String commandName = command;
        String[] commandArgs = args;

        switch(commandName){
            case "install":
                handleInstall(commandArgs[0]);
                break;
            case "restore":
                handleRestore(commandArgs[0]);
                break;
            case "get_needed_data":
                System.out.println("Get needed: " + handleGetNeededData());
                break;
            case "get_next_screen":
                System.out.println("Get screen: " + handleGetNextScreen());
                break;
            case "menu_input":
                System.out.println("Handle Menu: " + handleMenuInput(commandArgs[0]));
                break;
            case "form_input":
                System.out.println("Handle Form: " + handleFormInput(commandArgs[0]));
                break;
        }
    }

    private String handleFormInput(String commandArg) throws Exception{
        System.out.println("inputting");
        xFormPlayer.input(commandArg);
        System.out.println("inputted: " + commandArg);
        return handleFormPrompt();
    }

    private String handleMenuInput(String commandArg) throws Exception {
        Screen screen = SessionUtils.getNextScreen(sessionWrapper);
        screen.init(sessionWrapper);
        screen.handleInputAndUpdateSession(sessionWrapper, commandArg);
        return handleGetNextScreen();
    }

    private String handleGetNextScreen() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        try {
            Screen nextScreen = SessionUtils.getNextScreen(sessionWrapper);
            if(nextScreen == null){
                return handleFormEntry();
            }
            nextScreen.init(sessionWrapper);
            nextScreen.prompt(ps);
            return baos.toString("UTF8");
        } catch (CommCareSessionException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String handleFormPrompt() throws Exception{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        xFormPlayer.prompt(ps);
        return baos.toString("UTF8");
    }

    private String handleFormEntry() throws Exception {
        SessionUtils.printStack(sessionWrapper);
        xFormPlayer = SessionUtils.initFormEntry(sessionWrapper);
        return handleFormPrompt();
    }

    private String handleGetNeededData() {
        return sessionWrapper.getNeededData();
    }

    private void handleInstall(String profileReference) {
        sessionWrapper = SessionUtils.performInstall(profileReference);
    }

    private void handleRestore(String restoreFileReference){
        SessionUtils.performRestore(sessionWrapper.getSandbox(), restoreFileReference);
    }

}
