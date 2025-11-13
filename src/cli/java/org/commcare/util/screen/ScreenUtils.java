package org.commcare.util.screen;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.StackFrameStep;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.xpath.XPathException;

import java.util.Vector;

/**
 * Generally useful methods on CLI screens.
 *
 * Created by ctsims on 8/20/2015.
 */
public class ScreenUtils {

    public static void addPaddedStringToBuilder(StringBuilder builder, String s, int width) {
        if (s.length() > width) {
            builder.append(s, 0, width);
            return;
        }
        builder.append(s);
        if (s.length() != width) {
            // add whitespace padding
            for (int i = 0; i < width - s.length(); ++i) {
                builder.append(' ');
            }
        }
    }

    public static String pad(String s, int width) {
        StringBuilder builder = new StringBuilder();
        addPaddedStringToBuilder(builder, s, width);
        return builder.toString();
    }

    public static String getBestTitle(SessionWrapper session) {

        String[] stepTitles;
        try {
            stepTitles = session.getHeaderTitles();
        } catch (NoLocalizedTextException | XPathException e) {
            // localization resources may not be installed while in the middle of an update, so default to a
            // generic title
            // Also Catch XPathExceptions here since we don't want to show the xpath error on app startup and
            // these errors will be visible later to the user when they go to the respective menu
            return null;
        }

        Vector<StackFrameStep> v = session.getFrame().getSteps();

        // So we need to work our way backwards through each "step" we've taken, since our RelativeLayout
        // displays the Z-Order b insertion (so items added later are always "on top" of items added earlier
        String bestTitle = null;
        for (int i = v.size() - 1; i >= 0; i--) {
            if (bestTitle != null) {
                break;
            }
            StackFrameStep step = v.elementAt(i);

            if (!SessionFrame.STATE_DATUM_VAL.equals(step.getType())) {
                bestTitle = stepTitles[i];
            }
        }
        // If we didn't get a menu title, return the app title
        if (bestTitle == null) {
            return getAppTitle();
        }
        return bestTitle;
    }

    public static String getAppTitle() {
        try {
            return Localization.get("app.display.name");
        } catch (NoLocalizedTextException nlte) {
            return "CommCare";
        }
    }
}
