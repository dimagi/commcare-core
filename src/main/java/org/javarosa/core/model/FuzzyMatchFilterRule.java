package org.javarosa.core.model;

import org.commcare.modern.util.Pair;

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
                fuzzyMatch(textEnteredLowerCase, choiceLowerCase).first;
    }

    public static Pair<Boolean, Integer> fuzzyMatch(String source, String target) {
        //tweakable parameter: Minimum length before edit distance
        //starts being used (this is probably not necessary, and
        //basically only makes sure that "at" doesn't match "or" or similar
        if (source.length() > 3) {
            int distance = LevenshteinDistance(source, target);
            //tweakable parameter: edit distance past string length disparity
            if (distance <= 2) {
                return Pair.create(true, distance);
            }
        }
        return Pair.create(false, -1);
    }

    /**
     * Computes the Levenshtein Distance between two strings.
     * <p/>
     * This code is sourced and unmodified from wikibooks under
     * the Creative Commons attribution share-alike 3.0 license and
     * by be re-used under the terms of that license.
     * <p/>
     * http://creativecommons.org/licenses/by-sa/3.0/
     * <p/>
     * TODO: re-implement for efficiency/licensing possibly.
     */
    private static int LevenshteinDistance(String s0, String s1) {
        int len0 = s0.length() + 1;
        int len1 = s1.length() + 1;

        // the array of distances
        int[] cost = new int[len0];
        int[] newcost = new int[len0];

        // initial cost of skipping prefix in String s0
        for (int i = 0; i < len0; i++) cost[i] = i;

        // dynamicaly computing the array of distances

        // transformation cost for each letter in s1
        for (int j = 1; j < len1; j++) {

            // initial cost of skipping prefix in String s1
            newcost[0] = j - 1;

            // transformation cost for each letter in s0
            for (int i = 1; i < len0; i++) {

                // matching current letters in both strings
                int match = (s0.charAt(i - 1) == s1.charAt(j - 1)) ? 0 : 1;

                // computing cost for each transformation
                int cost_replace = cost[i - 1] + match;
                int cost_insert = cost[i] + 1;
                int cost_delete = newcost[i - 1] + 1;

                // keep minimum cost
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }

            // swap cost/newcost arrays
            int[] swap = cost;
            cost = newcost;
            newcost = swap;
        }

        // the distance is the cost for transforming all letters in both strings
        return cost[len0 - 1];
    }
}
