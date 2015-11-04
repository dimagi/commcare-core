package org.commcare.api.xml;

import org.commcare.api.engine.XFormPlayer;
import org.commcare.api.json.PromptToJson;
import org.commcare.api.screens.Screen;
import org.commcare.api.session.SessionUtils;
import org.commcare.api.session.SessionWrapper;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.parse.ParseUtils;
import org.commcare.api.session.CommCareSessionException;
import org.commcare.session.SessionFrame;
import org.javarosa.core.model.User;
import org.javarosa.core.services.storage.IStorageIterator;
import org.json.JSONException;
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
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by willpride on 10/27/15.
 */
public class XmlProcessor {

    SessionWrapper sessionWrapper;
    XFormPlayer xFormPlayer;

    boolean restored = false;
    boolean installed = false;

    String lastResponse = "";

    public String processRespondXML(File xmlRequest) throws Exception{
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlRequest);
        NodeList nodeList = doc.getElementsByTagName("command");

        sessionWrapper = null;
        xFormPlayer = null;
        String response = "";

        for(int i = 0; i < nodeList.getLength(); i++){
            Node node = nodeList.item(i);
            lastResponse = processCommandNode(node);
            if(lastResponse != null){
                System.out.println("Setting last response: " + lastResponse);
                response = lastResponse;
            }
        }
        return response;
    }

    private String processCommandNode(Node node) throws Exception {
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

        return handleCommandObject(commandName, returnArray);
    }

    private String handleCommandObject(String command, String[] args) throws Exception {
        String commandName = command;
        String[] commandArgs = args;

        System.out.println("Command: " + command + " argS: " + Arrays.toString(args));

        switch(commandName){
            case "install":
                if(!installed) {
                    installed = true;
                    return handleInstall(commandArgs[0]);
                } else{
                    return "Already installed";
                }
            case "restore":
                if(!restored){
                    restored = true;
                    return handleRestore(commandArgs[0]);
                } else{
                    return "Already restored";
                }
            case "get_needed_data":
                return handleGetNeededData();
            case "get_next_screen":
                return handleGetNextScreen();
            case "menu_input":
                return handleMenuInput(commandArgs[0]);
            case "form_input":
                return handleFormInput(commandArgs[0]);
            case "get_instance":
                return handleGetInstance();
            case "evaluate_xpath":
                return handleEvaluateXPath(commandArgs[0]);
            case "clear_state":
                return handleClear();
            case "assert_response":
                return handleAssertResponse(commandArgs[0]);
            case "get_json":
                return handleGetJson();
        }
        return "Command not recognized: " + command;
    }

    private String handleGetJson() {
        try {
            return PromptToJson.formEntryPromptToJson(xFormPlayer.getFormEntryController().getModel().getQuestionPrompt()).toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "null";
        }
    }

    private String handleAssertResponse(String commandArg) {
        if(commandArg.equals(lastResponse)){
            return "Assert is true";
        } else{
            return "Expected " + commandArg + " was " + lastResponse;
        }
    }

    private String handleClear() {
        sessionWrapper.clearAllState();
        return null;
    }

    private String handleEvaluateXPath(String commandArg) {
        return xFormPlayer.evalExpressionToString(commandArg);
    }

    private String handleGetInstance() {
        return xFormPlayer.getCurrentInstance();
    }

    private String handleFormInput(String commandArg) throws Exception{
        try {
            xFormPlayer.input(commandArg);
            return handleFormPrompt();
        } catch(XFormPlayer.InvalidInputException e){
            System.out.println("Invalid!!! " + e.getValue());
            return "Invalid input: " + e.getValue();
        }
    }

    private String handleMenuInput(String commandArg) throws Exception {
        Screen screen = SessionUtils.getNextScreen(sessionWrapper);
        screen.init(sessionWrapper);
        screen.handleInputAndUpdateSession(sessionWrapper, commandArg);
        return handleGetNextScreen();
    }

    private String handleGetNextScreen() throws Exception {
        try {
            Screen nextScreen = SessionUtils.getNextScreen(sessionWrapper);
            if(nextScreen == null){
                return handleFormEntry();
            }
            nextScreen.init(sessionWrapper);
            return nextScreen.getScreenXML();
        } catch (CommCareSessionException e) {
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

    private String handleInstall(String profileReference) {
        sessionWrapper = SessionUtils.performInstall(profileReference);
        return "Installed";
    }

    private String handleRestore(String restoreFileReference){
        SessionUtils.performRestore(sessionWrapper.getSandbox(), restoreFileReference);
        return "Restored";
    }

}
