package org.commcare.cases.entity;

import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.queryset.CurrentModelQuerySet;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.ReducingTraceReporter;
import org.javarosa.core.model.utils.InstrumentationUtils;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.List;

/**
 * @author ctsims
 */
public class NodeEntityFactory {
    private boolean mEntitySetInitialized = false;
    private static final Object mPreparationLock = new Object();

    protected final EvaluationContext ec;
    protected final Detail detail;
    private ReducingTraceReporter traceReporter;

    public NodeEntityFactory(Detail d, EvaluationContext ec) {
        this.detail = d;
        this.ec = ec;
    }

    public void activateDebugTraceOutput() {
        this.traceReporter = new ReducingTraceReporter();
    }

    public Detail getDetail() {
        return detail;
    }

    public Entity<TreeReference> getEntity(TreeReference data) {
        EvaluationContext nodeContext = new EvaluationContext(ec, data);
        if (traceReporter != null) {
            nodeContext.setDebugModeOn(traceReporter);
        }
        detail.populateEvaluationContextVariables(nodeContext);

        int length = detail.getHeaderForms().length;
        String extraKey = loadCalloutDataMapKey(nodeContext);

        Object[] fieldData = new Object[length];
        String[] sortData = new String[length];
        boolean[] relevancyData = new boolean[length];
        int count = 0;
        for (DetailField f : detail.getFields()) {
            try {
                fieldData[count] = f.getTemplate().evaluate(nodeContext);
                Text sortText = f.getSort();
                if (sortText == null) {
                    sortData[count] = null;
                } else {
                    sortData[count] = sortText.evaluate(nodeContext);
                }
                relevancyData[count] = f.isRelevant(nodeContext);
            } catch (XPathSyntaxException e) {
                /**
                 * TODO: 25/06/17 remove catch blocks from here
                 * We are wrapping the original exception in a new XPathException to avoid
                 * refactoring large number of functions caused by throwing XPathSyntaxException here.
                 */
                XPathException xe = new XPathException(e);
                throw xe;
            }
            count++;
        }

        return new Entity<>(fieldData, sortData, relevancyData, data, extraKey,
                detail.evaluateFocusFunction(nodeContext));
    }

    /**
     * Evaluate the lookup's 'template' detail block and use result as key for
     * attaching external (callout) data to the entity.
     */
    protected String loadCalloutDataMapKey(EvaluationContext entityContext) {
        if (detail.getCallout() != null) {
            DetailField calloutResponseDetail = detail.getCallout().getResponseDetailField();
            if (calloutResponseDetail != null) {
                Object extraDataKey = calloutResponseDetail.getTemplate().evaluate(entityContext);
                if (extraDataKey instanceof String) {
                    return (String)extraDataKey;
                }
            }
        }
        return null;
    }

    public List<TreeReference> expandReferenceList(TreeReference treeReference) {
        EvaluationContext tracableContext = new EvaluationContext(ec, ec.getOriginalContext());
        if (traceReporter != null) {
            tracableContext.setDebugModeOn(traceReporter);
        }
        List<TreeReference> result = tracableContext.expandReference(treeReference);
        printAndClearTraces("case load expand");

        setEvaluationContextDefaultQuerySet(ec, result);

        return result;
    }

    public void printAndClearTraces(String description) {
        if (traceReporter != null) {
            InstrumentationUtils.printAndClearTraces(traceReporter, description);
        }
    }

    /**
     * Lets the evaluation context know what the 'overall' query set in play is. This allows the
     * query planner to know that we aren't just looking to expand results for a specific element,
     * we're currently iterating over a potentially large set of elements and should batch
     * appropriately
     */
    protected void setEvaluationContextDefaultQuerySet(EvaluationContext ec,
                                                     List<TreeReference> result) {

        QueryContext newContext = ec.getCurrentQueryContext()
                .checkForDerivativeContextAndReturn(result.size());

        newContext.setHackyOriginalContextBody(new CurrentModelQuerySet(result));

        ec.setQueryContext(newContext);
    }

    /**
     * Performs the underlying work to prepare the entity set
     * (see prepareEntities()). Separated out to enforce timing
     * related to preparing and utilizing results
     * @param entities
     */
    protected void prepareEntitiesInternal(List<Entity<TreeReference>> entities) {
        //No implementation in normal factory
    }

    /**
     * Optional: Allows the factory to make all of the entities that it has
     * returned "Ready" by performing any lazy evaluation needed for optimum
     * usage. This preparation occurs asynchronously, and the returned entity
     * set should not be manipulated until it has completed.
     */
    public final void prepareEntities(List<Entity<TreeReference>> entities) {
        synchronized (mPreparationLock) {
            prepareEntitiesInternal(entities);
            mEntitySetInitialized = true;
        }
    }

    /**
     * Performs the underlying work to check on the entitySet preparation
     * (see isEntitySetReady()). Separated out to enforce timing
     * related to preparing and utilizing results
     */
    protected boolean isEntitySetReadyInternal() {
        return true;
    }

    /**
     * Called only after a call to prepareEntities, this signals whether
     * the entities returned are ready for bulk operations.
     *
     * @return True if entities returned from the factory are again ready
     * for use. False otherwise.
     */
    public final boolean isEntitySetReady() {
        synchronized (mPreparationLock) {
            if (!mEntitySetInitialized) {
                throw new RuntimeException("A Node Entity Factory was not prepared before usage. prepareEntities() must be called before a call to isEntitySetReady()");
            }
            return isEntitySetReadyInternal();
        }
    }
}
