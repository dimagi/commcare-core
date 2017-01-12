package org.javarosa.core.model;

import org.commcare.cases.util.StringUtils;

/**
 * Filter for rule for a combo box that accepts answer choice strings based on either
 * direct or fuzzy matches to the entered text
 */
public class FuzzyMatchFilterRule implements ComboboxFilterRule {

    @Override
    public boolean shouldRestrictTyping() {
        // Since fuzzy match only works once the number of typed letters reaches a certain
        // threshold and is close to the number of letters in the comparison string, it doesn't
        // make any sense to restrict typing here
        return false;
    }

    @Override
    public boolean choiceShouldBeShown(String choice, CharSequence textEntered) {
        if ("".equals(textEntered) || textEntered == null) {
            return true;
        }

        String textEnteredLowerCase = textEntered.toString().toLowerCase();
        String choiceLowerCase = choice.toLowerCase();

        // Try the easy cases first
        if (isSubstringOrFuzzyMatch(choiceLowerCase, textEnteredLowerCase)) {
            return true;
        }

        return allEnteredWordsHaveMatchOrFuzzyMatch(choiceLowerCase, textEnteredLowerCase);
    }

    private static boolean allEnteredWordsHaveMatchOrFuzzyMatch(String choiceLowerCase,
                                                                String textEnteredLowerCase) {
        String[] enteredWords = textEnteredLowerCase.split(" ");
        String[] wordsFromChoice = choiceLowerCase.split(" ");
        for (String enteredWord : enteredWords) {
            boolean foundMatchForWord = false;
            for (String wordFromChoice : wordsFromChoice) {
                if (isSubstringOrFuzzyMatch(wordFromChoice, enteredWord)) {
                    foundMatchForWord = true;
                    break;
                }
            }
            if (!foundMatchForWord) {
                return false;
            }
        }

        return true;
    }

    private static boolean isSubstringOrFuzzyMatch(String choiceLowerCase,
                                                   String textEnteredLowerCase) {
        return choiceLowerCase.contains(textEnteredLowerCase) ||
                StringUtils.fuzzyMatch(textEnteredLowerCase, choiceLowerCase).first;
    }
}
