package org.commcare.suite.model;

/**
 * Created by willpride on 4/13/16.
 */
public class Style {

    private DisplayFormat displayFormats;
    private int fontSize;
    private int widthHint;
    private String horizontalAlign;
    private String verticalAlign;

    private boolean showBorder;

    public Style(){}

    public Style(DetailField detail){
        if(detail.getFontSize() != null) {
            try {
                setFontSize(Integer.parseInt(detail.getFontSize()));
            } catch (NumberFormatException nfe) {
                setFontSize(12);
            }
        }
        // For width, default to -1 since '0' is reserved for hidden (Search) fiekds
        if(detail.getTemplateWidthHint() != null) {
            try {
                setWidthHint(Integer.parseInt(detail.getTemplateWidthHint()));
            } catch (NumberFormatException nfe) {
                setWidthHint(-1);
            }
        } else {
            setWidthHint(-1);
        }
        setDisplayFormatFromString(detail.getTemplateForm());

        verticalAlign = detail.getVerticalAlign();
        horizontalAlign = detail.getHorizontalAlign();
        showBorder = detail.getShowBorder();
    }

    enum DisplayFormat {
        Image,
        Audio,
        Text,
        Address,
        AddressPopup,
        Graph,
        Phone,
        Markdown,
        ClickableIcon,
    }

    public DisplayFormat getDisplayFormat() {
        return displayFormats;
    }

    private void setDisplayFormat(DisplayFormat displayFormats) {
        this.displayFormats = displayFormats;
    }

    public int getFontSize() {
        return fontSize;
    }

    private void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public int getWidthHint() {
        return widthHint;
    }

    private void setWidthHint(int widthHint) {
        this.widthHint = widthHint;
    }

    private void setDisplayFormatFromString(String displayFormat){
        switch (displayFormat) {
            case "image":
            case "enum-image":
                setDisplayFormat(DisplayFormat.Image);
                break;
            case "audio":
                setDisplayFormat(DisplayFormat.Audio);
                break;
            case "text":
                setDisplayFormat(DisplayFormat.Text);
                break;
            case "address":
                setDisplayFormat(DisplayFormat.Address);
                break;
            case "address-popup":
                setDisplayFormat(DisplayFormat.AddressPopup);
                break;
            case "graph":
                setDisplayFormat(DisplayFormat.Graph);
                break;
            case "phone":
                setDisplayFormat(DisplayFormat.Phone);
                break;
            case "markdown":
                setDisplayFormat(DisplayFormat.Markdown);
                break;
            case "clickable-icon":
                setDisplayFormat(DisplayFormat.ClickableIcon);
                break;
        }
    }

    @Override
    public String toString(){
        return "Style: [displayFormat=" + displayFormats + ", fontSize=" + fontSize + "]";
    }

    public String getHorizontalAlign() {
        return horizontalAlign;
    }

    public String getVerticalAlign() {
        return verticalAlign;
    }

    public boolean getShowBorder() {
        return showBorder;
    }
}
