package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import io.reactivex.Single;

/**
 * A Menu definition describes the structure of how
 * actions should be provided to the user in a CommCare
 * application.
 *
 * @author ctsims
 */
public class Menu implements Externalizable, MenuDisplayable {
    public static final String ROOT_MENU_ID = "root";
    public static final String TRAINING_MENU_ROOT = "training-root";

    private DisplayUnit display;
    private Vector<String> commandIds;
    private String[] commandExprs;
    private String id;
    private String root;
    private String rawRelevance;
    private String style;
    private XPathExpression relevance;
    AssertionSet assertions;

    /**
     * Serialization only!!!
     */
    public Menu() {

    }

    public Menu(String id, String root, String rawRelevance,
                XPathExpression relevance, DisplayUnit display,
                Vector<String> commandIds, String[] commandExprs,
                String style, AssertionSet assertions) {
        this.id = id;
        this.root = root;
        this.rawRelevance = rawRelevance;
        this.relevance = relevance;
        this.display = display;
        this.commandIds = commandIds;
        this.commandExprs = commandExprs;
        this.style = style;
        this.assertions = assertions;
    }

    /**
     * @return The ID of what menu an option to navigate to
     * this menu should be displayed in.
     */
    public String getRoot() {
        return root;
    }

    /**
     * @return A Text which should be displayed to the user as
     * the action which will display this menu.
     */
    public Text getName() {
        return display.getText();
    }

    /**
     * @return The ID of this menu. <p>If this value is "root"
     * many CommCare applications will support displaying this
     * menu's options at the app home screen</p>
     */
    public String getId() {
        return id;
    }

    /**
     * @return A parsed XPath expression that determines
     * whether or not to display this menu.
     */
    public XPathExpression getMenuRelevance() throws XPathSyntaxException {
        if (relevance == null && rawRelevance != null) {
            relevance = XPathParseTool.parseXPath(rawRelevance);
        }
        return relevance;
    }

    /**
     * @return A string representing an XPath expression to determine
     * whether or not to display this menu.
     */
    public String getMenuRelevanceRaw() {
        return rawRelevance;
    }

    /**
     * @return The ID of what command actions should be available
     * when viewing this menu.
     */
    public Vector<String> getCommandIds() {
        //UNSAFE! UNSAFE!
        return commandIds;
    }

    public XPathExpression getCommandRelevance(int index) throws XPathSyntaxException {
        //Don't cache this for now at all
        return commandExprs[index] == null ? null : XPathParseTool.parseXPath(commandExprs[index]);
    }

    public AssertionSet getAssertions() {
        return assertions == null ? new AssertionSet(new Vector<String>(), new Vector<Text>()) : assertions;
    }

    /**
     * @return an optional string indicating how this menu wants to display its items
     */
    public String getStyle() {
        return style;
    }

    /**
     * @param index the
     * @return the raw xpath string for a relevant condition (if available). Largely for
     * displaying to the user in the event of a failure
     */
    public String getCommandRelevanceRaw(int index) {
        return commandExprs[index];
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        id = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        root = ExtUtil.readString(in);
        rawRelevance = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        display = (DisplayUnit)ExtUtil.read(in, DisplayUnit.class, pf);
        commandIds = (Vector<String>)ExtUtil.read(in, new ExtWrapList(String.class), pf);
        commandExprs = new String[ExtUtil.readInt(in)];
        for (int i = 0; i < commandExprs.length; ++i) {
            if (ExtUtil.readBool(in)) {
                commandExprs[i] = ExtUtil.readString(in);
            }
        }
        style = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        assertions = (AssertionSet)ExtUtil.read(in, new ExtWrapNullable(AssertionSet.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(id));
        ExtUtil.writeString(out, root);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(rawRelevance));
        ExtUtil.write(out, display);
        ExtUtil.write(out, new ExtWrapList(commandIds));
        ExtUtil.writeNumeric(out, commandExprs.length);
        for (String commandExpr : commandExprs) {
            if (commandExpr == null) {
                ExtUtil.writeBool(out, false);
            } else {
                ExtUtil.writeBool(out, true);
                ExtUtil.writeString(out, commandExpr);
            }
        }

        ExtUtil.writeString(out, ExtUtil.emptyIfNull(style));
        ExtUtil.write(out, new ExtWrapNullable(assertions));
    }


    @Override
    public String getImageURI() {
        if (display.getImageURI() == null) {
            return null;
        }
        return display.getImageURI().evaluate();
    }

    @Override
    public String getAudioURI() {
        if (display.getAudioURI() == null) {
            return null;
        }
        return display.getAudioURI().evaluate();
    }

    @Override
    public String getDisplayText(EvaluationContext ec) {
        if (display.getText() == null) {
            return null;
        }
        return display.getText().evaluate(ec);
    }

    @Override
    public Single<String> getTextForBadge(final EvaluationContext ec) {
        if (display.getBadgeText() == null) {
            return Single.just("");
        }

        return display.getBadgeText().getDisposableSingleForEvaluation(ec);
    }

    @Override
    public Text getRawBadgeTextObject() {
        return display.getBadgeText();
    }

    @Override
    public Text getRawText() {
        return display.getText();
    }

    @Override
    public String getCommandID() {
        return id;
    }

    // unsafe! assumes that xpath expressions evaluate properly...
    public int indexOfCommand(String cmd) {
        return commandIds.indexOf(cmd);
    }

    @Override
    public String toString() {
        return "Menu with id " + this.getId();
    }

}
