/**
 *
 */
package org.commcare.cases.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A case index represents a link between one case and another. Depending
 * on the case's index relationship it affects which cases will be purged
 * from the phone when the user's scope is updated.
 *
 * @author ctsims
 */
public class CaseIndex implements Externalizable {

    /**
     * A Child index indicates that this case should ensure
     * that the indexed case is retained in the local scope
     * even if it is closed
     */
    public static final String RELATIONSHIP_CHILD = "child";

    /**
     * An extension case indicates that if the cases's parent is
     * closed out of the local scope, this case should be released
     * regardless of its status.
     */
    public static final String RELATIONSHIP_EXTENSION = "extension";

    protected String mName;
    protected String mTargetId;
    protected String mTargetCaseType;
    protected String mRelationship;

    /*
     * serialization only!
     */
    public CaseIndex() {

    }

    public CaseIndex(String name, String targetCaseType, String targetId) {
        this(name, targetCaseType, targetId, RELATIONSHIP_CHILD);
    }

    /**
     * Creates a case index
     *
     * @param name           The name of this index. Used for reference and lookup. A case may only have one
     *                       index with a given name
     * @param targetCaseType The case type of the target case
     * @param targetId       The ID value of the index. Should refer to another case.
     * @param relationship   The relationship between the indexing case and the indexed case. See
     *                       the RELATIONSHIP_* parameters of CaseIndex for more details.
     */
    public CaseIndex(String name, String targetCaseType, String targetId, String relationship) {
        this.mName = name;
        this.mTargetId = targetId;
        this.mTargetCaseType = targetCaseType;
        this.mRelationship = relationship;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        mName = ExtUtil.readString(in);
        mTargetId = ExtUtil.readString(in);
        mTargetCaseType = ExtUtil.readString(in);
        mRelationship = ExtUtil.readString(in);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, mName);
        ExtUtil.writeString(out, mTargetId);
        ExtUtil.writeString(out, mTargetCaseType);
        ExtUtil.writeString(out, mRelationship);
    }

    public String getName() {
        return mName;
    }

    public String getTargetType() {
        return mTargetCaseType;
    }

    public String getTarget() {
        return mTargetId;
    }

    public String getRelationship() {
        return mRelationship;
    }
}
