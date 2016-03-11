package org.commcare.util;

/**
 * Represents the stylistic attributes of an Entity
 * in a GridEntityView
 *
 * @author wspride
 */

public class GridStyle {
    private final String fontSize;
    private final String horzAlign;
    private final String vertAlign;
    private final String cssID;

    public GridStyle(String fs, String ha, String va, String cssid) {
        fontSize = fs;
        horzAlign = ha;
        vertAlign = va;
        cssID = cssid;
    }

    public String getFontSize() {
        if (fontSize == null) {
            return "normal";
        }
        return fontSize;
    }

    public String getHorzAlign() {
        if (horzAlign == null) {
            return "none";
        }
        return horzAlign;
    }

    public String getVertAlign() {
        if (vertAlign == null) {
            return "none";
        }
        return vertAlign;
    }

    public String getCssID() {
        if (cssID == null) {
            return "none";
        }
        return cssID;
    }

    public String toString() {
        return "font size: " + fontSize + ", horzAlign: " + horzAlign + ", vertAlign: " + vertAlign;
    }

}
