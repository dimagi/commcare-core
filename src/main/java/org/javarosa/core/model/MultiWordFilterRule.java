package org.javarosa.core.model;

/**
 * Filter for rule for a combo box that uses a more flexible filtering rule intended for when
 * answer choices are expected to contain multiple words.
 *
 * @author Aliza Stone
 */
public class MultiWordFilterRule implements ComboboxFilterRule {

    @Override
    public boolean shouldRestrictTyping() {
        return true;
    }

    /**
     *
     * @param choice - the answer choice to be considered
     * @param textEntered - the text entered by the user
     * @return true if choiceLowerCase contains any word within textEntered (the "words" of
     * textEntered are obtained by splitting textEntered on " ")
     */
    public boolean choiceShouldBeShown(String choice, CharSequence textEntered) {
        if ("".equals(textEntered) || textEntered == null) {
            return true;
        }
        String choiceLowerCase = choice.toLowerCase();
        String[] enteredTextIndividualWords = textEntered.toString().split(" ");
        for (String word : enteredTextIndividualWords) {
            if (!choiceLowerCase.contains(word.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
