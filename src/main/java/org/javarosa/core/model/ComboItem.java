package org.javarosa.core.model;

public class ComboItem {
    private String displayText;
    private String value;
    private int selectChoiceIndex;

    public ComboItem(String displayText, String value, int selectChoiceIndex) {
        this.displayText = displayText;
        this.value = value;
        this.selectChoiceIndex = selectChoiceIndex;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getValue() {
        return value;
    }

    public int getSelectChoiceIndex() {
        return selectChoiceIndex;
    }

    @Override
    public String toString() {
        return displayText;
    }

    @Override
    public boolean equals(Object obj) {
        ComboItem comboItem = (ComboItem)obj;
        return comboItem.getDisplayText().equals(displayText) && comboItem.getValue().equals(value) &&
                comboItem.getSelectChoiceIndex() == selectChoiceIndex;
    }
}
