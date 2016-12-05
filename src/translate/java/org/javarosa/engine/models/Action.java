package org.javarosa.engine.models;

/**
 * @author ctsims
 */
public class Action {
    private ActionResponse response;
    private Command command;
    private String input;

    public Action(String input) {
        this.input = input;
        response = ActionResponse.AnswerOk();
    }

    public Action(String input, ActionResponse response) {
        this.input = input;
        this.response = response;
    }

    public Action(Command command) {
        this.command = command;
    }

    public String getInputString() {
        if(input != null) {
            return input;
        } else {
            return ":" + command.getCommand();
        }
    }

    public String getRawAnswer() {
        return input;
    }

    public ActionResponse getActionResponse() {
        return response;
    }
}
