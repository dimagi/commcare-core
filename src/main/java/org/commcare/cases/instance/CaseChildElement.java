package org.commcare.cases.instance;

import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.PreloadUtils;

import java.util.Enumeration;
import java.util.Vector;

/**
 * @author ctsims
 */
public class CaseChildElement extends StorageBackedChildElement<Case> {

    private int recordId;
    private String caseId;

    private TreeElement empty;

    private static final String LAST_MODIFIED_KEY = "last_modified";

    public CaseChildElement(StorageInstanceTreeElement<Case, ?> parent,
                            int recordId, String caseId, int mult) {
        super(parent, mult);

        if (recordId == -1 && caseId == null) {
            throw new RuntimeException("Cannot create a lazy case element with no lookup identifiers!");
        }
        this.recordId = recordId;
        this.caseId = caseId;
        this.mult = mult;
    }

    /**
     * Template constructor (For elements that need to create reference nodesets
     * but never look up values)
     */
    private CaseChildElement(CaseInstanceTreeElement parent) {
        super(parent, TreeReference.INDEX_TEMPLATE);
        //Template
        this.recordId = TreeReference.INDEX_TEMPLATE;
        this.caseId = null;

        empty = new TreeElement("case");
        empty.setMult(this.mult);

        empty.setAttribute(null, "case_id", "");
        empty.setAttribute(null, "case_type", "");
        empty.setAttribute(null, "status", "");

        TreeElement scratch = new TreeElement("case_name");
        scratch.setAnswer(null);
        empty.addChild(scratch);

        scratch = new TreeElement("date_opened");
        scratch.setAnswer(null);
        empty.addChild(scratch);

        scratch = new TreeElement("last_modified");
        scratch.setAnswer(null);
        empty.addChild(scratch);
    }

    @Override
    public TreeElement getAttribute(String namespace, String name) {
        if (name.equals("case_id")) {
            if (recordId != TreeReference.INDEX_TEMPLATE) {
                //if we're already cached, don't bother with this nonsense
                synchronized (parent.treeCache) {
                    TreeElement element = parent.treeCache.retrieve(recordId);
                    if (element != null) {
                        return cache().getAttribute(namespace, name);
                    }
                }
            }

            //TODO: CACHE GET ID THING
            if (caseId == null) {
                return cache().getAttribute(namespace, name);
            }

            //otherwise, don't cache this just yet if we have the ID handy
            TreeElement caseid = TreeElement.constructAttributeElement(null, name);
            caseid.setValue(new StringData(caseId));
            caseid.setParent(this);
            return caseid;
        }
        return cache().getAttribute(namespace, name);
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        if (name.equals("case_id")) {
            return caseId;
        }
        return cache().getAttributeValue(namespace, name);
    }

    @Override
    public String getName() {
        return "case";
    }

    //TODO: THIS IS NOT THREAD SAFE
    @Override
    protected TreeElement cache() {
        if (recordId == TreeReference.INDEX_TEMPLATE) {
            return empty;
        }
        synchronized (parent.treeCache) {
            TreeElement element = parent.treeCache.retrieve(recordId);
            if (element != null) {
                return element;
            }
            //For now this seems impossible
            if (recordId == -1) {
                Vector<Integer> ids = parent.storage.getIDsForValue("case_id", caseId);
                recordId = ids.elementAt(0);
            }

            Case c = parent.getElement(recordId);
            caseId = c.getCaseId();
            TreeElement cacheBuilder = new TreeElement("case");
            cacheBuilder.setMult(this.mult);

            cacheBuilder.setAttribute(null, "case_id", c.getCaseId());
            cacheBuilder.setAttribute(null, "case_type", c.getTypeId());
            cacheBuilder.setAttribute(null, "status", c.isClosed() ? "closed" : "open");

            //Don't set anything to null
            cacheBuilder.setAttribute(null, "owner_id", c.getUserId() == null ? "" : c.getUserId());

            final boolean[] done = new boolean[]{false};

            TreeElement scratch = new TreeElement("case_name");
            String name = c.getName();
            //This shouldn't be possible
            scratch.setAnswer(new StringData(name == null ? "" : name));
            cacheBuilder.addChild(scratch);

            scratch = new TreeElement("date_opened");
            scratch.setAnswer(new DateData(c.getDateOpened()));
            cacheBuilder.addChild(scratch);

            scratch = new TreeElement(LAST_MODIFIED_KEY);
            scratch.setAnswer(new DateData(c.getLastModified()));
            cacheBuilder.addChild(scratch);

            setCaseProperties(c, cacheBuilder);

            TreeElement index = buildIndexTreeElement(c, done);
            cacheBuilder.addChild(index);

            TreeElement attachments = buildAttachmentTreeElement(c, done);
            cacheBuilder.addChild(attachments);

            cacheBuilder.setParent(this.parent);
            done[0] = true;

            parent.treeCache.register(recordId, cacheBuilder);

            return cacheBuilder;
        }
    }

    private void setCaseProperties(Case c, TreeElement cacheBuilder) {
        for (Enumeration en = c.getProperties().keys(); en.hasMoreElements(); ) {
            String key = (String)en.nextElement();

            //this is an unfortunate complication of our internal model
            if (LAST_MODIFIED_KEY.equals(key)) {
                continue;
            }

            TreeElement scratch = new TreeElement(key);
            Object temp = c.getProperty(key);
            if (temp instanceof String) {
                scratch.setValue(new UncastData((String)temp));
            } else {
                scratch.setValue(PreloadUtils.wrapIndeterminedObject(temp));
            }
            cacheBuilder.addChild(scratch, true);
        }
    }

    private TreeElement buildIndexTreeElement(Case c, final boolean[] done) {
        TreeElement index = new TreeElement("index") {
            @Override
            public TreeElement getChild(String name, int multiplicity) {
                TreeElement child = super.getChild(name, multiplicity);

                //TODO: Skeeeetchy, this is not a good way to do this,
                //should extract pattern instead.

                //If we haven't finished caching yet, we can safely not return
                //something useful here, so we can construct as normal.
                if (!done[0]) {
                    return child;
                }

                //blank template index for repeats and such to not crash
                if (multiplicity >= 0 && child == null) {
                    TreeElement emptyNode = new TreeElement(name);
                    emptyNode.setAttribute(null, "case_type", "");
                    emptyNode.setAttribute(null, "relationship", "");
                    this.addChild(emptyNode);
                    emptyNode.setParent(this);
                    return emptyNode;
                }
                return child;
            }

            @Override
            public Vector<TreeElement> getChildrenWithName(String name) {
                Vector<TreeElement> children = super.getChildrenWithName(name);

                //If we haven't finished caching yet, we can safely not return
                //something useful here, so we can construct as normal.
                if (!done[0]) {
                    return children;
                }

                if (children.size() == 0) {
                    TreeElement emptyNode = new TreeElement(name);
                    emptyNode.setAttribute(null, "case_type", "");
                    emptyNode.setAttribute(null, "relationship", "");

                    this.addChild(emptyNode);
                    emptyNode.setParent(this);
                    children.addElement(emptyNode);
                }
                return children;
            }
        };

        Vector<CaseIndex> indices = c.getIndices();
        for (CaseIndex i : indices) {
            TreeElement scratch = new TreeElement(i.getName());
            scratch.setAttribute(null, "case_type", i.getTargetType());
            scratch.setAttribute(null, "relationship", i.getRelationship());
            scratch.setValue(new UncastData(i.getTarget()));
            index.addChild(scratch);
        }
        return index;
    }

    private TreeElement buildAttachmentTreeElement(Case c, final boolean[] done) {
        TreeElement attachments = new TreeElement("attachment") {
            @Override
            public TreeElement getChild(String name, int multiplicity) {
                TreeElement child = super.getChild(name, multiplicity);

                //TODO: Skeeeetchy, this is not a good way to do this,
                //should extract pattern instead.

                //If we haven't finished caching yet, we can safely not return
                //something useful here, so we can construct as normal.
                if (!done[0]) {
                    return child;
                }
                if (multiplicity >= 0 && child == null) {
                    TreeElement emptyNode = new TreeElement(name);
                    this.addChild(emptyNode);
                    emptyNode.setParent(this);
                    return emptyNode;
                }
                return child;
            }
        };

        for (String attachment : c.getAttachments()) {
            TreeElement scratch = new TreeElement(attachment);
            scratch.setValue(new UncastData(c.getAttachmentSource(attachment)));
            attachments.addChild(scratch);
        }
        return attachments;
    }

    public static CaseChildElement buildCaseChildTemplate(CaseInstanceTreeElement parent) {
        return new CaseChildElement(parent);
    }
}
