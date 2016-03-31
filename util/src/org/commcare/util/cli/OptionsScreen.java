package org.commcare.util.cli;

/**
 * An OptionsScreen provides a set of options (IE modules, list, cases) that can be selected
 * to advance through menu entry
 */
public interface OptionsScreen {
    String[] getOptions();
}
