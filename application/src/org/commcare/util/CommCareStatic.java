/**
 *
 */
package org.commcare.util;

import org.commcare.resources.model.MissingMediaException;
import org.commcare.resources.model.ResourceTable;
import org.commcare.view.CommCareStartupInteraction;
import org.javarosa.core.model.instance.TreeReferenceLevel;
import org.javarosa.core.util.Interner;
import org.javarosa.core.util.SizeBoundUniqueVector;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.xform.parse.XFormParserFactory;
import org.javarosa.xform.util.XFormUtils;
import org.javarosa.xpath.expr.XPathStep;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author ctsims
 *
 */
public class CommCareStatic {
    //Holds all static reference stuff
    private static Interner<TreeReferenceLevel> treeRefLevels;
    private static Interner<XPathStep> xpathSteps;
    public static Interner<String> appStringCache;

    public static void init() {
        treeRefLevels = new Interner<TreeReferenceLevel>();
        xpathSteps = new Interner<XPathStep>();
        appStringCache= new Interner<String>();
        TreeReferenceLevel.attachCacheTable(treeRefLevels);
        XPathStep.attachInterner(xpathSteps);
        ExtUtil.attachCacheTable(appStringCache);

        XFormUtils.setXFormParserFactory(new XFormParserFactory(appStringCache));
    }

    public static void cleanup() {
        //TODO: This doesn't do anything, we need to, like, tell them to clean up their internals instead.
        treeRefLevels = null;
        xpathSteps = null;
        appStringCache = null;
    }

    public static String validate(ResourceTable mResourceTable){

        SizeBoundUniqueVector<MissingMediaException> problems = new SizeBoundUniqueVector<MissingMediaException>(10);

        mResourceTable.verifyInstallation(problems);
        if(problems.size() > 0 ) {
            int badImageRef = problems.getBadImageReferenceCount();
            int badAudioRef = problems.getBadAudioReferenceCount();
            int badVideoRef = problems.getBadVideoReferenceCount();
            String errorMessage    = "CommCare cannot start because you are missing multimedia files.";
            String message = CommCareStartupInteraction.failSafeText("install.bad",errorMessage, new String[] {""+badImageRef,""+badAudioRef,""+badVideoRef});
            Hashtable<String, Vector<String>> problemList = new Hashtable<String,Vector<String>>();
            for(Enumeration en = problems.elements() ; en.hasMoreElements() ;) {
                MissingMediaException mme = (MissingMediaException)en.nextElement();

                String res = mme.getResource().getResourceId();

                Vector<String> list;
                if(problemList.containsKey(res)) {
                    list = problemList.get(res);
                } else{
                    list = new Vector<String>();
                }

                // code to pretty up the output for mealz

                int substringIndex = mme.getMessage().indexOf("/commcare");

                String shortenedMessage = (mme.getMessage()).substring(substringIndex+1);

                list.addElement(shortenedMessage);

                problemList.put(res, list);

            }

            message += "\n-----------";

            for(Enumeration en = problemList.keys(); en.hasMoreElements();) {

                String resource = (String)en.nextElement();
                //message += "\n-----------";
                for(String s : problemList.get(resource)) {
                    message += "\n" + s;
                }
            }
            if(problems.getAdditional() > 0) {
                message += "\n\n..." + problems.getAdditional() + " more";
            }

            return message;
        }
        return null;
    }
}
