package org.javarosa.core.model;

/**
 * Filter for rule for a combo box that accepts answer choice strings based on simple prefix logic
 *
 * @author Aliza Stone
 */
public class StandardFilterRule implements ComboboxFilterRule {

    @Override
    public boolean shouldRestrictTyping() {
        return true;
    }

    @Override
    public boolean choiceShouldBeShown(String choice, CharSequence textEntered) {
        return choice.toLowerCase().startsWith(textEntered.toString().toLowerCase());
    }
}
