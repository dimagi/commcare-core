package org.commcare.xml;

/**
 * listener for any changes to case indexes due to processing a case transaction
 */
public interface CaseIndexChangeListener {


    /**
     * A signal that notes that processing a transaction has resulted in a
     * potential change in what cases should be on the phone. This can be
     * due to a case's owner changing, a case closing, an index moving, etc.
     *
     * Does not have to be consumed, but can be used to identify proactively
     * when to reconcile what cases should be available.
     *
     * @param caseId The ID of a case which has changed in a potentially
     *               disruptive way
     */
    abstract void onIndexDisrupted(String caseId);
}
