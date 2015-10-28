/**
 *
 */
package org.commcare.api.engine.models;

/**
 * @author ctsims
 *
 */
public class Command {
    String command;
    public Command(String command) {
        this.command = command;
    }
    public String getCommand() {
        return command;
    }
}
