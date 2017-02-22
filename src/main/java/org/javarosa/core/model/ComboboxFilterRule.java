package org.javarosa.core.model;

/**
 * @author Aliza Stone
 */
public interface ComboboxFilterRule {

    /**
     *
     * @param choice - an answer choice available in this adapter
     * @param textEntered - the text entered by the user in the combobox's edittext field
     * @return If the given choice should be displayed in combobox's dropdown menu, based upon
     * the text that the user currently has entered
     */
    boolean choiceShouldBeShown(String choice, CharSequence textEntered);


    /**
     * @return Whether the text that a user can type into the corresponding combobox's edittext
     * field should be restricted in accordance with its adapter's filtering rules
     */
    boolean shouldRestrictTyping();
}
