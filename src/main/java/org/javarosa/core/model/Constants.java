package org.javarosa.core.model;


/**
 * Constants shared throught classes in the containing package.
 */
public class Constants {
    /**
     * ID not set to a value
     */
    public static final int NULL_ID = -1;

    public static final int DATATYPE_UNSUPPORTED = -1;
    public static final int DATATYPE_NULL = 0;  /* for nodes that have no data, or data type otherwise unknown */
    public static final int DATATYPE_TEXT = 1;
    /**
     * Text question type.
     */
    public static final int DATATYPE_INTEGER = 2;
    /**
     * Numeric question type. These are numbers without decimal points
     */
    public static final int DATATYPE_DECIMAL = 3;
    /**
     * Decimal question type. These are numbers with decimals
     */
    public static final int DATATYPE_DATE = 4;
    /**
     * Date question type. This has only date component without time.
     */
    public static final int DATATYPE_TIME = 5;
    /**
     * Time question type. This has only time element without date
     */
    public static final int DATATYPE_DATE_TIME = 6;
    /**
     * Date and Time question type. This has both the date and time components
     */
    public static final int DATATYPE_CHOICE = 7;
    /**
     * This is a question with alist of options where not more than one option can be selected at a time.
     */
    public static final int DATATYPE_CHOICE_LIST = 8;
    /**
     * This is a question with alist of options where more than one option can be selected at a time.
     */
    public static final int DATATYPE_BOOLEAN = 9;
    /**
     * Question with true and false answers.
     */
    public static final int DATATYPE_GEOPOINT = 10;
    /**
     * Question with location answer.
     */
    public static final int DATATYPE_BARCODE = 11;
    /**
     * Question with barcode string answer.
     */
    public static final int DATATYPE_BINARY = 12;
    /**
     * Question with external binary answer.
     */
    public static final int DATATYPE_LONG = 13;
    /**
     * Question with external binary answer.
     */

    public static final int CONTROL_UNTYPED = -1;
    public static final int CONTROL_INPUT = 1;
    public static final int CONTROL_SELECT_ONE = 2;
    public static final int CONTROL_SELECT_MULTI = 3;
    public static final int CONTROL_TEXTAREA = 4;
    public static final int CONTROL_SECRET = 5;
    public static final int CONTROL_RANGE = 6;
    public static final int CONTROL_UPLOAD = 7;
    public static final int CONTROL_SUBMIT = 8;
    public static final int CONTROL_TRIGGER = 9;
    public static final int CONTROL_IMAGE_CHOOSE = 10;
    public static final int CONTROL_LABEL = 11;
    public static final int CONTROL_AUDIO_CAPTURE = 12;
    public static final int CONTROL_VIDEO_CAPTURE = 13;
    public static final int CONTROL_MICRO_IMAGE = 14;

    /**
     * constants for xform tags
     */
    public static final String XFTAG_UPLOAD = "upload";

    /**
     * constants for stack frame step extras
     */
    public static final String EXTRA_POST_SUCCESS = "post-success";
}
