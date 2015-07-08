/**
 *
 */
package org.commcare.entity;

import java.util.Vector;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.Iterator;
import org.javarosa.entity.model.EntitySet;
import org.javarosa.j2me.view.ProgressIndicator;
import org.javarosa.model.xform.XPathReference;

/**
 * NOTE: Definitely not thread-safe.
 *
 * @author ctsims
 *
 */
public class NodeEntitySet implements EntitySet<TreeReference>, ProgressIndicator {

    private Vector<TreeReference> set = null;
    private TreeReference path;
    private EvaluationContext context;

    public NodeEntitySet(TreeReference path, EvaluationContext context) {
        this.path = path;
        this.context = context;
    }

    public int getCount() {
        load();
        return set.size();
    }

    public TreeReference get(int index) {
        load();
        //TODO: How to index this properly? Not absolute...
        return set.elementAt(index);
    }

    public Iterator<TreeReference> iterate() {
        load();
        return new VectorIterator<TreeReference>(set);
    }

    public int getId(TreeReference e) {
        load();
        for(int i = 0; i < set.size() ; ++i) {
            if(e == set.elementAt(i)) {
                return i;
            }
        }
        return -1;
    }

    public double getProgress() {
        if(loadingDetails == null ) { return 0; }
        return loadingDetails[0] / (double)loadingDetails[1];
    }

    public String getCurrentLoadingStatus() {
        return Localization.get("node.select.filtering");
    }

    public int getIndicatorsProvided() {
        return ProgressIndicator.INDICATOR_PROGRESS | ProgressIndicator.INDICATOR_STATUS;
    }

    int[] loadingDetails;
    private void load() {
        if(set == null) {
            loadingDetails = new int[]{0, 1};
            context.setPredicateProcessSet(loadingDetails);
            set = context.expandReference(path);
            //don't need these anymore
            path = null;
            context = null;
            loadingDetails = null;
        }
    }

    public boolean loaded() {
        return set != null;
    }

}
