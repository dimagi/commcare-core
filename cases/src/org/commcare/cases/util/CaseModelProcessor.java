/**
 *
 */
package org.commcare.cases.util;

import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.InvalidIndexException;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.xform.util.XFormAnswerDataSerializer;

import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.Vector;

/**
 * @author ctsims
 */
public class CaseModelProcessor implements ICaseModelProcessor {

    XFormAnswerDataSerializer serializer = new XFormAnswerDataSerializer();
    Case c;

    public Case getCase() {
        return c;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.utils.IInstanceProcessor#processModel(org.javarosa.core.model.instance.FormInstance)
     */
    public void processInstance(FormInstance tree) {
        Vector caseElements = scrapeForCaseElements(tree);
        for (int i = 0; i < caseElements.size(); ++i) {
            try {
                processCase((TreeElement)caseElements.elementAt(i));
            } catch (MalformedCaseModelException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    private void processCase(TreeElement caseElement) throws MalformedCaseModelException {
        c = null;
        TreeElement caseIdAttribute = caseElement.getAttribute(null, "case_id");
        if (caseIdAttribute == null) {
            throw new MalformedCaseModelException("Invalid <case> model. <case> element requires case_id attribute at :" + caseElement.getRef().toString(true), "<case>");
        }
        if (caseIdAttribute.getValue() == null) {
            throw new MalformedCaseModelException("Invalid <case> model. case_id attribute contains no value at " + caseElement.getRef().toString(true), "<case>");
        }
        String caseId = caseIdAttribute.getValue().uncast().getString();

        if (caseId == null || caseId == "") {
            throw new MalformedCaseModelException("Invalid <case> model. <case> element case_id is ''! at:" + caseElement.getRef().toString(true), "<case>");
        }

        TreeElement dateModified = caseElement.getAttribute(null, "date_modified");
        if (dateModified == null) {
            throw new MalformedCaseModelException("Invalid <case> model. <case> element requires date_modified attribute at :" + caseElement.getRef().toString(true), "<case>");
        }
        if (dateModified.getValue() == null) {
            throw new MalformedCaseModelException("Invalid <case> model. date_modified attribute contains no value at " + caseElement.getRef().toString(true), "<case>");
        }
        //TODO: Do we need to cast here?
        Date date = (Date)(new DateTimeData().cast(dateModified.getValue().uncast())).getValue();

        for (int i = 0; i < caseElement.getNumChildren(); ++i) {
            TreeElement kid = caseElement.getChildAt(i);
            if (!caseElement.isRelevant()) {
                continue;
            }
            if (kid.getName().equals("create")) {
                if (kid.isRelevant()) {
                    c = processCaseCreate(kid, caseId, date);
                }
            } else if (kid.getName().equals("update")) {
                if (c == null) {
                    c = getCase(caseId);
                }
                if (kid.isRelevant()) {
                    processCaseMutate(kid, c, date);
                }
            } else if (kid.getName().equals("close")) {
                if (c == null) {
                    c = getCase(caseId);
                }
                if (kid.isRelevant()) {
                    processCaseClose(kid, c, date);
                }
            } else if (kid.getName().equals("index")) {
                if (c == null) {
                    c = getCase(caseId);
                }
                if (kid.isRelevant()) {
                    processCaseIndex(kid, c, date);
                }
            } else if (kid.getName().equals("attachment")) {
                if (c == null) {
                    c = getCase(caseId);
                }
                if (kid.isRelevant()) {
                    processCaseAttachments(kid, c, date);
                }
            }
        }
    }

    private Case getCase(String id) {
        IStorageUtilityIndexed storage = (IStorageUtilityIndexed)StorageManager.getStorage(Case.STORAGE_KEY);

        try {
            Case c = (Case)storage.getRecordForValue("case-id", id);
            return c;
        } catch (NoSuchElementException e) {
            //We eventually probably want to deal with this. For now, it's a dealbreaker. Throw it up.
            e.printStackTrace();
            throw e;
        } catch (InvalidIndexException iie) {
            //We eventually probably want to deal with this. For now, it's a dealbreaker. Throw it up.
            iie.printStackTrace();
            throw iie;
        }
    }

    private void commit(Case c, Date lastModified) {
        IStorageUtility utility = StorageManager.getStorage(Case.STORAGE_KEY);
        utility.write(c);
    }

    private Case processCaseCreate(TreeElement create, String caseId, Date lastModified) throws MalformedCaseModelException {
        String caseTypeId = null;
        String extId = null;
        String caseName = null;
        String userId = null;

        for (int i = 0; i < create.getNumChildren(); ++i) {
            TreeElement kid = create.getChildAt(i);
            if (!kid.isRelevant()) {
                continue;
            }
            try {
                if (kid.getName().equals("case_type")) {
                    caseTypeId = kid.getValue().uncast().getString();
                    continue;
                }
                if (kid.getName().equals("owner_id")) {
                    userId = kid.getValue().uncast().getString();
                    continue;
                }
                if (kid.getName().equals("case_name")) {
                    caseName = kid.getValue().uncast().getString();
                    continue;
                }
            } catch (NullPointerException npe) {
                throw new MalformedCaseModelException("Invalid <create> model, null value for included element: " + kid.getRef().toString(true), "<create>");
            }

        }

        if (caseTypeId == null || caseName == null) {
            throw new MalformedCaseModelException("Invalid <create> model. Required element is missing.", "<create>");
        }
        Case c = new Case(caseName, caseTypeId);
        c.setCaseId(caseId);
        c.setDateOpened(lastModified);
        if (userId != null) {
            c.setUserId(userId);
        }
        commit(c, lastModified);
        Logger.log("case-create", c.getID() + ";" + PropertyUtils.trim(c.getCaseId(), 12) + ";" + c.getTypeId());
        return c;
    }

    private void processCaseMutate(TreeElement mutate, Case c, Date lastModified) throws MalformedCaseModelException {
        for (int i = 0; i < mutate.getNumChildren(); ++i) {
            TreeElement kid = mutate.getChildAt(i);
            if (!kid.isRelevant()) {
                continue;
            }
            try {
                if (kid.getName().equals("case_type")) {
                    c.setTypeId(kid.getValue().uncast().getString());
                    continue;
                } else if (kid.getName().equals("case_name")) {
                    c.setName(kid.getValue().uncast().getString());
                    continue;
                } else if (kid.getName().equals("date_opened")) {
                    c.setDateOpened((Date)(kid.getValue().getValue()));
                    continue;
                } else if (kid.getName().equals("owner_id")) {
                    c.setUserId(kid.getValue().uncast().getString());
                    continue;
                }
            } catch (NullPointerException npe) {
                //kind of a crude catchall here
                throw new MalformedCaseModelException("Invalid update element attempting to set required data to null at " + kid.getRef().toString(true), "<update>");
            }
            //Otherwise...
            String vname = kid.getName();

            //We skip nodes which aren't relevant above by completely ignoring them. If a node has a null value, that means
            //that it exists and is simply empty, so we need to set a valid property vlaue for it (empty string), so that
            //properties can be overriden.
            String value = "";
            if (kid.getValue() != null) {
                value = kid.getValue().uncast().getString();
            }
            c.setProperty(vname, value);
        }
        commit(c, lastModified);
    }

    private void processCaseClose(TreeElement close, Case c, Date lastModified) throws MalformedCaseModelException {
        c.setClosed(true);
        commit(c, lastModified);
        Logger.log("case-close", PropertyUtils.trim(c.getCaseId(), 12));
    }

    private void processCaseIndex(TreeElement index, Case c, Date lastModified) throws MalformedCaseModelException {
        boolean modified = false;
        for (int i = 0; i < index.getNumChildren(); ++i) {
            TreeElement child = index.getChildAt(i);
            if (!child.isRelevant()) {
                continue;
            }
            String indexName = child.getName();
            String indexType = child.getAttributeValue(null, "case_type");
            String relationship = child.getAttributeValue(null, "relationship");
            if (relationship == null) {
                relationship = CaseIndex.RELATIONSHIP_CHILD;
            }
            IAnswerData data = child.getValue();
            if (data == null) {
                c.removeIndex(indexName);
            } else {
                c.setIndex(new CaseIndex(indexName, indexType, data.uncast().getString(), relationship));
            }
            modified = true;
        }
        if (modified) {
            commit(c, lastModified);
        }
    }

    private void processCaseAttachments(TreeElement attachments, Case c, Date lastModified) throws MalformedCaseModelException {
        boolean modified = false;
        for (int i = 0; i < attachments.getNumChildren(); ++i) {
            TreeElement attachment = attachments.getChildAt(i);
            if (!attachment.isRelevant()) {
                continue;
            }

            String name = attachment.getName();
            String src = attachment.getAttributeValue(null, "src");

            String value = this.localizeAttachment(name, src);

            if (value != null) {
                c.updateAttachment(name, value);
                modified = true;
            }
        }
        if (modified) {
            commit(c, lastModified);
        }
    }

    protected String localizeAttachment(String name, String src) {
        return "jr://file" + src.substring(src.lastIndexOf('/'));
    }

    private Vector scrapeForCaseElements(FormInstance tree) {
        Vector caseElements = new Vector();

        Stack children = new Stack();
        children.push(tree.getRoot());
        while (!children.empty()) {
            TreeElement element = (TreeElement)children.pop();
            for (int i = 0; i < element.getNumChildren(); ++i) {
                TreeElement caseElement = element.getChildAt(i);
                if (!caseElement.isRelevant() || caseElement.getMult() == TreeReference.INDEX_TEMPLATE) {
                    continue;
                }
                if (caseElement.getName().equals("case")) {
                    caseElements.addElement(caseElement);
                } else {
                    children.push(caseElement);
                }
            }
        }
        return caseElements;
    }
}
