/**
 *
 */
package org.commcare.entity;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.Constants;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.util.DataUtil;
import org.javarosa.entity.model.Entity;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;

import java.lang.Object;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author ctsims
 *
 */
public class CommCareEntity extends Entity<TreeReference> {

    Detail shortDetail;
    Detail longDetail;
    String[] shortText;
    String[] sortText;
    EvaluationContext context;
    NodeEntitySet set;

    public CommCareEntity(Detail shortDetail, Detail longDetail, EvaluationContext context, NodeEntitySet set) {
        this.shortDetail = shortDetail;
        this.longDetail = longDetail;
        this.context = context;
        this.set = set;
    }

    protected int readEntityId(TreeReference element) {
        return set.getId(element);
    }

    /* (non-Javadoc)
     * @see org.javarosa.entity.model.Entity#entityType()
     */
    public String entityType() {
        return Localizer.clearArguments(shortDetail.getTitle().getText().evaluate(context));
    }

    /* (non-Javadoc)
     * @see org.javarosa.entity.model.Entity#factory()
     */
    public Entity<TreeReference> factory() {
        CommCareEntity entity = new CommCareEntity(shortDetail,longDetail, context, set);
        return entity;
    }

    /* (non-Javadoc)
     * @see org.javarosa.entity.model.Entity#getHeaders(boolean)
     */
    public String[] getHeaders(boolean detailed) {
        Detail d;
        if(!detailed) {
            d = shortDetail;
        } else{
            if(longDetail == null) { return null;}
            d = longDetail;
        }

        if (d.getNodeset() != null) {
            throw new RuntimeException("Entity subnodes not supported: " + d.getNodeset().toString());
        }

        Detail[] details = getFlattenedDetails(d);
        String[] output = new String[getFlattenedFieldCount(details)];
        int i = 0;
        for (int j = 0; j < details.length; j++) {
            for (int k = 0; k < details[j].getFields().length; k++) {
                output[i] = details[j].getFields()[k].getHeader().evaluate();
                i++;
            }
        }
        return output;
    }

    /* (non-Javadoc)
     * @see org.javarosa.patient.select.activity.IEntity#matchID(java.lang.String)
     */
    public boolean match (String key) {
        key = key.toLowerCase();
        String[] fields = this.getShortFields();
        for(int i = 0; i < fields.length; ++i) {
            // don't match to images or graphs
            String form = shortDetail.getFields()[i].getTemplateForm();
            if(form.equals("image") || form.equals("graph")) {
                continue;
            }
            if(fields[i].toLowerCase().startsWith(key)) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.entity.model.Entity#getForms(boolean)
     */
    public String[] getForms(boolean header) {
        return header ? shortDetail.getHeaderForms() : shortDetail.getTemplateForms();
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.entity.model.Entity#getForms(boolean)
     */
    public String[] getLongForms(boolean header) {
        if(longDetail == null) { return null;}
        Detail[] details = getFlattenedDetails(longDetail);
        String[] allForms = new String[getFlattenedFieldCount(details)];
        int k = 0;
        for (int i = 0; i < details.length; i++) {
            String[] forms = header ? details[i].getHeaderForms() : details[i].getTemplateForms();
            for (int j = 0; j < forms.length; j++) {
                allForms[k] = forms[j];
                k++;
            }
        }
        return allForms;
    }

    /* (non-Javadoc)
     * @see org.javarosa.entity.model.Entity#getLongFields(org.javarosa.core.services.storage.Persistable)
     */
    public String[] getLongFields(TreeReference element) {
        if(longDetail == null) { return null;}
        EvaluationContext ec = new EvaluationContext(context, element);
        loadVars(ec, longDetail);
        Detail[] details = getFlattenedDetails(longDetail);
        String[] output = new String[getFlattenedFieldCount(details)];
        int k = 0;
        for (int i = 0; i < details.length; i++) {
            for (int j = 0; j < details[i].getFields().length; j++) {
                Object template = details[i].getFields()[j].getTemplate();
                if (template instanceof Text) {
                    output[k] = ((Text) template).evaluate(ec);
                } else {
                    output[k] = "";
                }
                k++;
            }
        }
        return output;
    }

    /* (non-Javadoc)
     * @see org.javarosa.entity.model.Entity#getShortFields()
     */
    public String[] getShortFields() {
        return shortText;
    }

    public String[] getStyleHints (boolean header) {
        if(header) {
            return shortDetail.getHeaderSizeHints();
        } else {
            return shortDetail.getTemplateSizeHints();
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.entity.model.Entity#loadEntity(org.javarosa.core.services.storage.Persistable)
     */
    protected void loadEntity(TreeReference element) {
        EvaluationContext ec = new EvaluationContext(context, element);
        loadVars(ec, shortDetail);
        loadTexts(ec);
    }

    /**
     * Given a detail, return an array of details that will contain either
     * - all child details
     * - a single-element array containing the given detail, if it has no children
     * @param d The detail to flatten.
     */
    private Detail[] getFlattenedDetails(Detail d) {
        if (d.isCompound()) {
            return d.getDetails();
        }
        return new Detail[] {d};
    }

    private int getFlattenedFieldCount(Detail[] details) {
        int count = 0;
        for (int i = 0; i < details.length; i++) {
            count += details[i].getFields().length;
        }
        return count;
    }

    private void loadVars(EvaluationContext ec, Detail detail) {
        Detail[] details = getFlattenedDetails(detail);
        Hashtable<String, XPathExpression> decs = new Hashtable<String, XPathExpression>();
        for (int i = 0; i < details.length; i++) {
            for (Enumeration en = details[i].getVariableDeclarations().keys(); en.hasMoreElements(); ) {
                String key = (String)en.nextElement();
                decs.put(key, details[i].getVariableDeclarations().get(key));
            }
        }

        for(Enumeration en = decs.keys() ; en.hasMoreElements();) {
            String key = (String)en.nextElement();
            try {
                ec.setVariable(key, XPathFuncExpr.unpack(decs.get(key).eval(ec)));
            } catch(XPathException xpe) {
                xpe.printStackTrace();
                throw new RuntimeException("XPathException while parsing varible " + key+ " for entity. " +  xpe.getMessage());
            }
        }
    }

    private void loadTexts(EvaluationContext context) {
        shortText = new String[shortDetail.getFields().length];
        sortText = new String[shortDetail.getFields().length];
        for(int i = 0 ; i < shortText.length ; ++i) {
            Object template = shortDetail.getFields()[i].getTemplate();
            if (template instanceof Text) {
                shortText[i] = ((Text) template).evaluate(context);
            }
            else {
                shortText[i] = "";
            }

            //see whether or not the field has a special text form just for sorting
            Text sortKey = shortDetail.getFields()[i].getSort();
            if(sortKey == null) {
                //If not, we'll just use the display text
                sortText[i] = shortText[i];
            } else {
                //If so, evaluate it
                sortText[i] = sortKey.evaluate(context);
            }
        }
    }

    /**
     * Get a list of what the default sort orderings should be for this entity
     * @return An array of indices into the getSortFields() array specifying
     * the order in which sorting should be applied by default
     */
    public int[] getDefaultSortOrder() {
        return shortDetail.getSortOrder();
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.entity.model.Entity#getSortFields()
     */
    public int[] getSortFields () {
        int[] sortOrder = getDefaultSortOrder();
        Vector<Integer> fields = new Vector<Integer>();

        //Put the default sorted ones in at the top
        for(int index : sortOrder) {
            fields.addElement(DataUtil.integer(index));
        }

        //now loop through the rest and see if they need to get added
        String[] headers = getHeaders(false);
        for(int i = 0 ; i < headers.length ; ++i) {
            if(headers[i] == null  || headers[i].equals("")) { continue;}
            //if it's already added, don't re-add it
            if(fields.contains(DataUtil.integer(i))) {
                //already present
            } else {
                fields.addElement(DataUtil.integer(i));
            }
        }
        int[] ret = new int[fields.size()];
        for(int i = 0; i < fields.size() ; ++i) {
            ret[i] = fields.elementAt(i).intValue();
        }
        return ret;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.entity.model.Entity#getSortFieldNames()
     */
    public String getSortFieldName (int index) {
        return getHeaders(false)[index];
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.entity.model.Entity#getSortKey(java.lang.String)
     */
    public Object getSortKey (int index) {
        //Get the sort value
        String text = sortText[index];

        //Figure out if we need to cast to a type for comparison
        int sortType = shortDetail.getFields()[index].getSortType();

        try {
            if(sortType == Constants.DATATYPE_TEXT) {
                return text.toLowerCase();
            } else if(sortType == Constants.DATATYPE_INTEGER) {
                //Double -> int is comprable
                return XPathFuncExpr.toInt(text);
            } else if(sortType == Constants.DATATYPE_DECIMAL) {
                return XPathFuncExpr.toDouble(text);
            } else {
                //Hrmmmm :/ Handle better?
                return text;
            }
        } catch(XPathTypeMismatchException e) {
            //So right now this will fail 100% silently, which is bad, but
            //I find it very likely that people are going to mess this up
            //constantly...
            Logger.log("config", "Entity Select: Couldn't cast "+ text + " to datatype " + sortType + "|" + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the orientation that the specified field should be sorted in
     *
     * @param fieldKey The key to be sorted
     * @return true if the field should be sorted in ascending order. False otherwise
     */
    public boolean isSortAscending(int index) {
        return !(shortDetail.getFields()[index].getSortDirection() == DetailField.DIRECTION_DESCENDING);
    }
}
