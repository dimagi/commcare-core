package org.javarosa.form.api;

import org.commcare.cases.util.StringUtils;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.services.locale.Localizer;

import java.util.Hashtable;
import java.util.Vector;

/**
 * This class gives you all the information you need to display a caption when
 * your current FormIndex references a GroupEvent, RepeatPromptEvent, or
 * RepeatEvent.
 *
 * @author Simon Kelly
 */
public class FormEntryCaption {

    FormDef form;
    FormIndex index;
    protected IFormElement element;
    private String textID;

    public static final String TEXT_FORM_LONG = "long";
    public static final String TEXT_FORM_SHORT = "short";
    public static final String TEXT_FORM_AUDIO = "audio";
    public static final String TEXT_FORM_IMAGE = "image";
    public static final String TEXT_FORM_VIDEO = "video";
    public static final String TEXT_FORM_MARKDOWN = "markdown";

    /**
     * This empty constructor exists for convenience of any supertypes of this
     * prompt
     */
    public FormEntryCaption() {
    }

    /**
     * Creates a FormEntryCaption for the element at the given index in the form.
     */
    public FormEntryCaption(FormDef form, FormIndex index) {
        this.form = form;
        this.index = index;
        this.element = form.getChild(index);
        this.textID = this.element.getTextID();
    }


    /**
     * Convenience method
     * Get longText form of text for THIS element (if available)
     * !!Falls back to default form if 'long' form does not exist.!!
     * Use getSpecialFormQuestionText() if you want short form only.
     *
     * @return longText form
     */
    public String getLongText() {
        return getQuestionText(getTextID());
    }

    /**
    * Convenience method
    * Get shortText form of text for THIS element (if available)
    * !!Falls back to default form if 'short' form does not exist.!!
    * Use getSpecialFormQuestionText() if you want short form only.
    *
    * @return shortText form
    */
    public String getShortText() {
        String returnText = getSpecialFormQuestionText(getTextID(), TEXT_FORM_SHORT);
        if (returnText == null) {
            returnText = getLongText();
            }
        return returnText;
    }

    /**
     * Convenience method
     * Get audio URI from Text form for THIS element (if available)
     *
     * @return audio URI form stored in current locale of Text, returns null if not available
     */
    public String getAudioText() {
        return getSpecialFormQuestionText(getTextID(), TEXT_FORM_AUDIO);
    }

    /**
     * Convenience method
     * Get image URI form of text for THIS element (if available)
     *
     * @return URI of image form stored in current locale of Text, returns null if not available
     */
    public String getImageText() {
        return getSpecialFormQuestionText(getTextID(), TEXT_FORM_IMAGE);
    }
    /**
     * Convenience method
     * Get video URI form of text for THIS element (if available)
     *
     * @return URI of video form stored in current locale of Text, returns null if not available
     */
    public String getVideoText() {
        return this.getSpecialFormQuestionText(this.getTextID(), "video");
    }

    /**
     * Convenience method
     * Get the markdown styled text (if available)
     *
     * @return Text with MarkDown styling
     */
    public String getMarkdownText() {
        return getSpecialFormQuestionText(getTextID(), TEXT_FORM_MARKDOWN);
    }

    /**
     * Attempts to return question text for this element.
     * Will check for text in the following order:<br/>
     * Localized Text (long form) -> Localized Text (no special form) <br />
     * If no textID is specified, method will return THIS element's labelInnerText.
     *
     * @param textID - The textID of the text you're trying to retrieve. if <code>textID == null</code> will get LabelInnerText for current element
     * @return Question Text.  <code>null</code> if no text for this element exists (after all fallbacks).
     * @throws RunTimeException if this method is called on an element that is NOT a QuestionDef
     */
    public String getQuestionText(String textID) {
        String tid = textID;
        if ("".equals(tid)) {
            tid = null; //to make things look clean
        }

        //check for the null id case and return labelInnerText if it is so.
        if (tid == null) return substituteStringArgs(element.getLabelInnerText());

        //otherwise check for 'long' form of the textID, then for the default form and return
        String returnText;
        returnText = getIText(tid, "long");
        if (returnText == null) returnText = getIText(tid, null);

        return substituteStringArgs(returnText);
    }

    /**
     * Same as getQuestionText(String textID), but for the current element textID;
     *
     * @return Question Text
     * @see getQuestionText(String textID)
     */
    public String getQuestionText() {
        return getQuestionText(getTextID());
    }

    /**
     * This method is generally used to retrieve special forms of a
     * textID, e.g. "audio", "video", etc.
     *
     * @param textID - The textID of the text you're trying to retrieve.
     * @param form   - special text form of textID you're trying to retrieve.
     * @return Special Form Question Text. <code>null</code> if no text for this element exists (with the specified special form).
     * @throws RunTimeException if this method is called on an element that is NOT a QuestionDef
     */
    public String getSpecialFormQuestionText(String textID, String form) {
        if (textID == null || textID.equals("")) return null;

        String returnText = getIText(textID, form);

        return substituteStringArgs(returnText);
    }

    /**
     * Same as getSpecialFormQuestionText(String textID,String form) except that the
     * textID defaults to the textID of the current element.
     *
     * @param form - special text form of textID you're trying to retrieve.
     * @return Special Form Question Text. <code>null</code> if no text for this element exists (with the specified special form).
     * @throws RunTimeException if this method is called on an element that is NOT a QuestionDef
     */
    public String getSpecialFormQuestionText(String form) {
        return getSpecialFormQuestionText(getTextID(), form);
    }


    /**
     * @param textID - the textID of the text you'd like to retrieve
     * @param form   - the special form (e.g. "audio","long", etc) of the text
     * @return the IText for the parameters specified.
     */
    protected String getIText(String textID, String form) {
        String returnText = null;
        if (textID == null || textID.equals("")) return null;
        if (form != null && !form.equals("")) {
            try {
                returnText = localizer().getRawText(localizer().getLocale(), textID + ";" + form);
            } catch (NullPointerException npe) {
            }
        } else {
            try {
                returnText = localizer().getRawText(localizer().getLocale(), textID);
            } catch (NullPointerException npe) {
            }
        }
        return returnText;
    }

    //TODO: this is explicitly missing integration with the new multi-media support
    //TODO: localize the default captions
    public String getRepeatText(String typeKey) {
        GroupDef g = (GroupDef)element;
        if (!g.isRepeat()) {
            throw new RuntimeException("not a repeat");
        }

        String title = getLongText();
        int count = getNumRepetitions();

        String caption = null;
        if ("mainheader".equals(typeKey)) {
            caption = getCaptionText(g.mainHeader);
            if (caption == null) {
                return title;
            }
        } else if ("add".equals(typeKey)) {
            caption = getCaptionText(g.addCaption);
            if (caption == null) {
                return "Add another " + title;
            }
        } else if ("add-empty".equals(typeKey)) {
            caption = getCaptionText(g.addEmptyCaption);
            if (caption == null) {
                caption = g.addCaption;
            }
            if (caption == null) {
                return "None - Add " + title;
            }
        } else if ("del".equals(typeKey)) {
            caption = getCaptionText(g.delCaption);
            if (caption == null) {
                return "Delete " + title;
            }
        } else if ("done".equals(typeKey)) {
            caption = getCaptionText(g.doneCaption);
            if (caption == null) {
                return "Done";
            }
        } else if ("done-empty".equals(typeKey)) {
            caption = getCaptionText(g.doneEmptyCaption);
            if (caption == null) {
                caption = g.doneCaption;
            }
            if (caption == null) {
                return "Skip";
            }
        } else if ("delheader".equals(typeKey)) {
            caption = getCaptionText(g.delHeader);
            if (caption == null) {
                return "Delete which " + title + "?";
            }
        }

        Hashtable<String, Object> vars = new Hashtable<>();
        vars.put("name", title);
        vars.put("n", Integer.valueOf(count));
        return form.fillTemplateString(caption, index.getReference(), vars);
    }

    private String getCaptionText(String textIdOrText) {
        if (!StringUtils.isEmpty(textIdOrText)) {
            String returnText = getIText(textIdOrText, null);
            if (returnText != null) {
                return substituteStringArgs(returnText);
            }
        }
        return substituteStringArgs(textIdOrText);
    }

    //this should probably be somewhere better
    public int getNumRepetitions() {
        return form.getNumRepetitions(index);
    }

    public String getRepetitionText(boolean newrep) {
        return getRepetitionText("header", index, newrep);
    }

    private String getRepetitionText(String type, FormIndex index, boolean newrep) {
        if (element instanceof GroupDef && ((GroupDef)element).isRepeat() && index.getElementMultiplicity() >= 0) {
            GroupDef g = (GroupDef)element;

            String title = getLongText();
            int ix = index.getElementMultiplicity() + 1;
            int count = getNumRepetitions();

            String caption = null;
            if ("header".equals(type)) {
                caption = g.entryHeader;
            } else if ("choose".equals(type)) {
                caption = g.chooseCaption;
                if (caption == null) {
                    caption = g.entryHeader;
                }
            }
            if (caption == null) {
                return title == null ? ix + "/" + count : title + " " + ix + "/" + count;
            }

            Hashtable<String, Object> vars = new Hashtable<>();
            vars.put("name", title);
            vars.put("i", ix);
            vars.put("n", count);
            vars.put("new", newrep);
            return form.fillTemplateString(caption, index.getReference(), vars);
        } else {
            return null;
        }
    }

    public Vector<String> getRepetitionsText() {
        GroupDef g = (GroupDef)element;
        if (!g.isRepeat()) {
            throw new RuntimeException("not a repeat");
        }

        int numRepetitions = getNumRepetitions();
        Vector<String> reps = new Vector<>();
        for (int i = 0; i < numRepetitions; i++) {
            reps.addElement(getRepetitionText("choose", form.descendIntoRepeat(index, i), false));
        }
        return reps;
    }

    public static class RepeatOptions {
        public String header;
        public String add;
        public String delete;
        public String done;
        public String delete_header;
    }

    /**
     * Used by touchforms
     */
    @SuppressWarnings("unused")
    public RepeatOptions getRepeatOptions() {
        RepeatOptions ro = new RepeatOptions();
        boolean has_repetitions = (getNumRepetitions() > 0);

        ro.header = getRepeatText("mainheader");

        ro.add = null;
        if (form.canCreateRepeat(form.getChildInstanceRef(index), index)) {
            ro.add = getRepeatText(has_repetitions ? "add" : "add-empty");
        }
        ro.delete = null;
        ro.delete_header = null;
        if (has_repetitions) {
            ro.delete = getRepeatText("del");
            ro.delete_header = getRepeatText("delheader");
        }
        ro.done = getRepeatText(has_repetitions ? "done" : "done-empty");

        return ro;
    }

    public String getAppearanceHint() {
        return element.getAppearanceAttr();
    }

    protected String substituteStringArgs(String templateStr) {
        if (templateStr == null) {
            return null;
        }
        return form.fillTemplateString(templateStr, index.getReference());
    }

    public int getMultiplicity() {
        return index.getElementMultiplicity();
    }

    public IFormElement getFormElement() {
        return element;
    }

    /**
     * @return true if this represents a <repeat> element
     */
    public boolean repeats() {
        if (element instanceof GroupDef) {
            return ((GroupDef)element).isRepeat();
        } else {
            return false;
        }
    }

    public FormIndex getIndex() {
        return index;
    }

    public Localizer localizer() {
        return this.form.getLocalizer();
    }

    protected String getTextID() {
        return this.textID;
    }
}
