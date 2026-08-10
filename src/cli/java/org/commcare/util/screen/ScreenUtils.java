package org.commcare.util.screen;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.suite.model.Suite;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.xpath.XPathException;

import java.util.List;
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

    public static String getMenuTitle(SessionWrapper session) {
        // Walk backwards looking for the most recent step that is an actual menu
        Vector<StackFrameStep> steps = session.getFrame().getSteps();
        for (int i = steps.size() - 1; i >= 0; i--) {
            StackFrameStep step = steps.elementAt(i);
            if (!SessionFrame.STATE_COMMAND_ID.equals(step.getType())) {
                continue;
            }

            for (Suite s : session.getPlatform().getInstalledSuites()) {
                List<Menu> menus = s.getMenusWithId(step.getId());
                if (menus == null) {
                    continue;
                }
                for (Menu m : menus) {
                    Text name = m.getName();
                    if (name == null) {
                        continue;
                    }
                    try {
                        // Menus contain a potential argument listing where that value is on the screen,
                        // clear it out if it exists
                        return Localizer.processArguments(name.evaluate(), new String[]{""}).trim();
                    } catch (NoLocalizedTextException | XPathException e) {
                        // localization resources may not be installed while in the middle of an update,
                        // or the menu's title may reference session state unavailable here; either way
                        // this is just for logging, so fall back to no title instead of blowing up.
                        return null;
                    }
                }
            }
        }

        return null;
    }

    public static String getAppTitle() {
        try {
            return Localization.get("app.display.name");
        } catch (NoLocalizedTextException nlte) {
            return "CommCare";
        }
    }

    public static class HereDummyFunc implements IFunctionHandler {
        private final double lat;
        private final double lon;

        public HereDummyFunc(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public String getName() {
            return "here";
        }

        @Override
        public Vector getPrototypes() {
            Vector<Class[]> p = new Vector<>();
            p.addElement(new Class[0]);
            return p;
        }

        @Override
        public boolean rawArgs() {
            return false;
        }

        @Override
        public Object eval(Object[] args, EvaluationContext ec) {
            return new GeoPointData(new double[]{lat, lon, 0, 10}).getDisplayText();
        }
    }
}
