package org.javarosa.core.model.condition;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;

import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.QuerySensitiveTreeElementWrapper;
import org.commcare.cases.query.queryset.CurrentModelQuerySet;
import org.commcare.cases.util.QueryUtils;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ConcreteInstanceRoot;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.TreeUtilities;
import org.javarosa.core.model.trace.BulkEvaluationTrace;
import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.core.model.trace.EvaluationTraceReporter;
import org.javarosa.core.model.utils.CacheHost;
import org.javarosa.core.services.Logger;
import org.javarosa.xpath.IExprDataType;
import org.javarosa.xpath.XPathLazyNodeset;
import org.javarosa.xpath.XPathMissingInstanceException;
import org.javarosa.xpath.expr.ExpressionCacher;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;


/**
 * A collection of objects that affect the evaluation of an expression, like
 * function handlers and (not supported) variable bindings.
 */
public class EvaluationContext {

    /**
     * Whether XPath expressions being evaluated should be traced during
     * execution for debugging.
     */
    private boolean mAccumulateExprs = false;

    /**
     * During debugging this context is the base that holds the trace root and
     * aggregates ongoing execution.
     */
    private EvaluationContext debugContext;

    /**
     * The current execution trace being evaluated in debug mode
     */
    private EvaluationTrace currentTraceLevel = null;

    /**
     * The root of the current execution trace
     */
    private EvaluationTrace traceRoot = null;

    /**
     * An optional reporter for traced evaluations
     */
    private EvaluationTraceReporter traceReporter = null;

    // Unambiguous anchor reference for relative paths
    private final TreeReference contextNode;

    private final Hashtable<String, IFunctionHandler> functionHandlers;
    private final Hashtable<String, Object> variables;

    // Do we want to evaluate constraints?
    public boolean isConstraint;

    // validate this value when isConstraint is set
    public IAnswerData candidateValue;

    // Responsible for informing itext what form is requested if relevant
    private String outputTextForm = null;

    private final Hashtable<String, DataInstance> formInstances;

    // original context reference used for evaluating current()
    private TreeReference original;

    // Keeps track of the overall context for the executing query stack
    private QueryContext queryContext;

    // Used to disable reference cache maintained by a TreeElement node
    private boolean useReferenceCache = true;

    /**
     * What element in a nodeset is the context currently pointing to?
     * Used for calculating the position() xpath function.
     */
    private int currentContextPosition = -1;

    private ExpressionCacher expressionCacher;

    private final DataInstance instance;

    public EvaluationContext(DataInstance instance) {
        this(instance, new Hashtable<>());
    }

    public EvaluationContext(EvaluationContext base, TreeReference context) {
        this(base, base.instance, context, base.formInstances);
    }

    public EvaluationContext(EvaluationContext base,
                             Hashtable<String, DataInstance> formInstances,
                             TreeReference context) {
        this(base, base.instance, context, formInstances);
    }

    public EvaluationContext(FormInstance instance,
                             Hashtable<String, DataInstance> formInstances,
                             EvaluationContext base) {
        this(base, instance, base.contextNode, formInstances);
    }

    public EvaluationContext(DataInstance instance,
                             Hashtable<String, DataInstance> formInstances) {
        this.formInstances = formInstances;
        this.instance = instance;
        this.contextNode = TreeReference.rootRef();
        functionHandlers = new Hashtable<>();
        variables = new Hashtable<>();
        this.setQueryContext(new QueryContext());
    }

    /**
     * Copy Constructor
     */
    private EvaluationContext(EvaluationContext base, DataInstance instance, TreeReference contextNode,
                              Hashtable<String, DataInstance> formInstances) {
        //TODO: These should be deep, not shallow
        this.functionHandlers = base.functionHandlers;

        this.formInstances = new Hashtable<>();
        this.copyInstances(formInstances);

        this.variables = new Hashtable<>();
        //TODO: this is actually potentially much slower than
        //our old strategy (but is needed for this object to
        //be threadsafe). We should evaluate the potential impact.
        this.shallowVariablesCopy(base.variables);

        this.contextNode = contextNode;
        this.instance = instance;

        this.isConstraint = base.isConstraint;
        this.candidateValue = base.candidateValue;

        this.outputTextForm = base.outputTextForm;
        this.original = base.original;

        //Hrm....... not sure about this one. this only happens after a rescoping,
        //and is fixed on the context. Anything that changes the context should
        //invalidate this
        this.currentContextPosition = base.currentContextPosition;

        if (base.mAccumulateExprs) {
            this.mAccumulateExprs = true;
            this.debugContext = base.debugContext;
        }

        this.expressionCacher = base.expressionCacher;
        setQueryContext(base.queryContext);
        this.useReferenceCache = base.useReferenceCache;
    }

    public DataInstance getInstance(String id) {
        return formInstances.containsKey(id) ? formInstances.get(id) : null;
    }

    public TreeReference getContextRef() {
        return contextNode;
    }

    public void setOriginalContext(TreeReference ref) {
        this.original = ref;
    }

    public TreeReference getOriginalContext() {
        if (this.original == null) {
            return this.contextNode;
        } else {
            return this.original;
        }
    }

    public void enableExpressionCaching() {
        this.expressionCacher = new ExpressionCacher();
    }

    public boolean expressionCachingEnabled() {
        return expressionCacher != null;
    }

    public ExpressionCacher expressionCacher() {
        return expressionCacher;
    }

    public void addFunctionHandler(IFunctionHandler fh) {
        functionHandlers.put(fh.getName(), fh);
    }

    public Hashtable<String, IFunctionHandler> getFunctionHandlers() {
        return functionHandlers;
    }

    public void setOutputTextForm(String form) {
        this.outputTextForm = form;
    }

    public String getOutputTextForm() {
        return outputTextForm;
    }

    private void shallowVariablesCopy(Hashtable<String, Object> variablesToCopy) {
        for (Enumeration e = variablesToCopy.keys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            variables.put(key, variablesToCopy.get(key));
        }
    }

    /**
     * This is not a true deep copy since it does not copy the underlying data structures,
     * but it does isolate some changes to the instances which happen when spawning new contexts
     * e.g. replacing the root.
     */
    private void copyInstances(Hashtable<String, DataInstance> formInstances) {
        if (formInstances != null) {
            for (Map.Entry<String, DataInstance> entry : formInstances.entrySet()) {
                DataInstance inst = entry.getValue();
                if (inst instanceof ExternalDataInstance) {
                    inst = ((ExternalDataInstance)inst).copy();
                }
                this.formInstances.put(entry.getKey(), inst);
            }
        }
    }

    public void setVariables(Hashtable<String, ?> variables) {
        for (Enumeration e = variables.keys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            setVariable(key, variables.get(key));
        }
    }

    public void setVariable(String name, Object value) {
        //No such thing as a null xpath variable. Empty
        //values in XPath just get converted to ""
        if (value == null||
                (value instanceof String && value.toString().trim().isEmpty())) {
            variables.put(name, "");
            return;
        }
        //Otherwise check whether the value is one of the normal first
        //order datatypes used in xpath evaluation
        if (value instanceof Boolean ||
                value instanceof Double ||
                value instanceof String ||
                value instanceof Date ||
                value instanceof IExprDataType) {
            variables.put(name, value);
            return;
        }

        //Some datatypes can be trivially converted to a first order
        //xpath datatype
        if (value instanceof Integer) {
            variables.put(name, Double.valueOf(((Integer)value).doubleValue()));
            return;
        }
        if (value instanceof Float) {
            variables.put(name, Double.valueOf(((Float)value).doubleValue()));
        } else {
            //Otherwise we just hope for the best, I suppose? Should we log this?
            variables.put(name, value);
        }
    }

    public Object getVariable(String name) {
        return variables.get(name);
    }

    public QueryContext getCurrentQueryContext() {
        return queryContext;
    }

    public void setQueryContext(QueryContext queryContext) {
        this.queryContext = queryContext;
        queryContext.setTraceRoot(this);
    }

    public Vector<TreeReference> expandReference(TreeReference ref) {
        return expandReference(ref, false);
    }

    /**
     * Search for all repeated nodes that match the pattern of the 'ref'
     * argument.
     *
     * '/' returns {'/'}
     * can handle sub-repetitions (e.g., {/a[1]/b[1], /a[1]/b[2], /a[2]/b[1]})
     *
     * @param ref Potentially ambiguous reference
     * @return Null if 'ref' is relative reference. Otherwise, returns a vector
     * of references that point to nodes that match 'ref' argument. These
     * references are unambiguous (no index will ever be INDEX_UNBOUND) template
     * nodes won't be included when matching INDEX_UNBOUND, but will be when
     * INDEX_TEMPLATE is explicitly set.
     */
    public Vector<TreeReference> expandReference(TreeReference ref, boolean includeTemplates) {
        if (!ref.isAbsolute()) {
            return null;
        }

        DataInstance baseInstance = retrieveInstance(ref);
        Vector<TreeReference> v = new Vector<>();

        expandReferenceAccumulator(ref, baseInstance, baseInstance.getRoot().getRef(useReferenceCache), v, includeTemplates);
        return v;
    }

    /**
     * Recursive helper function for expandReference that performs the search
     * for all repeated nodes that match the pattern of the 'ref' argument.
     *
     * @param sourceRef      original path we're matching against
     * @param sourceInstance original node obtained from sourceRef
     * @param workingRef     explicit path that refers to the current node
     * @param refs           Accumulator vector to collect matching paths. Contained
     *                       references are unambiguous. Template nodes won't be included when
     *                       matching INDEX_UNBOUND, but will be when INDEX_TEMPLATE is explicitly
     *                       set.
     */
    private void expandReferenceAccumulator(TreeReference sourceRef, DataInstance sourceInstance,
                                            TreeReference workingRef, Vector<TreeReference> refs,
                                            boolean includeTemplates) {

        if (workingRef == null) {
            throw new RuntimeException("Encountered invalid instance definition while evaluating " + sourceRef.toString() +
                    " for instance " + sourceInstance.getInstanceId() + " with root: " + sourceInstance.getRoot());
        }

        int depth = workingRef.size();

        if (depth == sourceRef.size()) {
            // We've matched fully
            //TODO: Should this reference be cloned?
            refs.addElement(workingRef);
            return;
        }
        // Get the next set of matching references
        String name = sourceRef.getName(depth);
        int mult = sourceRef.getMultiplicity(depth);
        Vector<XPathExpression> predicates = sourceRef.getPredicate(depth);
        Vector<XPathExpression> originalPredicates = predicates;

        // Batch fetch is going to mutate the predicates vector, create a copy
        if (predicates != null) {
            Vector<XPathExpression> predCopy = new Vector<>(predicates.size());
            for (XPathExpression xpe : predicates) {
                predCopy.addElement(xpe);
            }
            predicates = predCopy;
        }

        AbstractTreeElement node = sourceInstance.resolveReference(workingRef, this);

        this.openBulkTrace();

        // Use the reference's simple predicates to filter the potential
        // nodeset.  Predicates used in filtering are removed from the
        // predicate input argument.
        Collection<TreeReference> childSet = node.tryBatchChildFetch(name, mult, predicates, this);

        this.reportBulkTraceResults(originalPredicates, predicates, childSet);
        this.closeTrace();

        if (childSet == null) {
            childSet = loadReferencesChildren(node, name, mult, includeTemplates);
        }

        QueryContext subContext = queryContext.
                checkForDerivativeContextAndReturn(childSet == null ? 0 : childSet.size());

        // If we forked a new query body from above (IE: a new large query) and there wasn't an
        // original context before, we can anticipate that the subcontext below will reference
        // into the returned body as the original context, which is ugly, but opens up
        // intense optimizations
        if (this.getOriginalContextForPropogation() == null && subContext != queryContext) {
            subContext.setHackyOriginalContextBody(new CurrentModelQuerySet(childSet));
        }

        // Create a place to store the current position markers
        int[] positionContext = new int[predicates == null ? 0 : predicates.size()];

        for (TreeReference refToExpand : childSet) {
            boolean passedAll = true;
            if (predicates != null && predicates.size() > 0) {
                // Evaluate and filter predicates not processed by
                // tryBatchChildFetch
                int predIndex = -1;
                for (XPathExpression predExpr : predicates) {
                    predIndex++;
                    // Just by getting here we're establishing a position for
                    // evaluating the current context. If we break, we won't
                    // push up the next one
                    positionContext[predIndex]++;

                    EvaluationContext evalContext = rescope(refToExpand, positionContext[predIndex],
                            subContext);
                    Object o = predExpr.eval(sourceInstance, evalContext);
                    o = FunctionUtils.unpack(o);

                    boolean passed = false;
                    if (o instanceof Double) {
                        // If a predicate expression is just an Integer, check
                        // if its equal to the current position context

                        // The spec just says "number" for when to use this;
                        // Not clear what to do with a non-integer/rounding.
                        int intVal = FunctionUtils.toInt(o).intValue();
                        passed = (intVal == positionContext[predIndex]);
                    } else if (o instanceof Boolean) {
                        passed = (Boolean)o;
                    }

                    if (!passed) {
                        passedAll = false;
                        break;
                    }
                }
            }
            if (passedAll) {
                expandReferenceAccumulator(sourceRef, sourceInstance, refToExpand, refs, includeTemplates);
            }
        }
    }

    /**
     * Gather references to a nodes children with a specific name and
     * multiplicity.
     *
     * @param node             Element of which to collect child references.
     * @param childName        Only collect child references with this name.
     * @param childMult        Collect a particular element/attribute or unbounded.
     * @param includeTemplates Should the result include template elements?
     * @return A list of references to a node's children that have a given name
     * and multiplicity.
     */
    private Vector<TreeReference> loadReferencesChildren(AbstractTreeElement node,
                                                         String childName,
                                                         int childMult,
                                                         boolean includeTemplates) {
        Vector<TreeReference> childSet = new Vector<>();
        QueryUtils.prepareSensitiveObjectForUseInCurrentContext(node, getCurrentQueryContext());

        node = QuerySensitiveTreeElementWrapper.WrapWithContext(node, getCurrentQueryContext());
        //NOTE: This currently won't propogate the wrapped context.

        if (node.hasChildren()) {
            if (childMult == TreeReference.INDEX_UNBOUND) {
                int count = node.getChildMultiplicity(childName);
                for (int i = 0; i < count; i++) {
                    AbstractTreeElement child = node.getChild(childName, i);
                    if (child != null) {
                        childSet.addElement(child.getRef(useReferenceCache));
                    } else {
                        throw new IllegalStateException("Missing or non-sequential nodes expanding a reference: " + node.getRef(
                                useReferenceCache));
                    }
                }
                if (includeTemplates) {
                    AbstractTreeElement template = node.getChild(childName, TreeReference.INDEX_TEMPLATE);
                    if (template != null) {
                        childSet.addElement(template.getRef(useReferenceCache));
                    }
                }
            } else if (childMult != TreeReference.INDEX_ATTRIBUTE) {
                // TODO: Make this test childMult >= 0?
                // If the multiplicity is a simple integer, just get the
                // appropriate child
                AbstractTreeElement child = node.getChild(childName, childMult);
                if (child != null) {
                    childSet.addElement(child.getRef(useReferenceCache));
                }
            }
        }

        // Working reference points to an attribute; add it to set to
        // process
        if (childMult == TreeReference.INDEX_ATTRIBUTE) {
            AbstractTreeElement attribute = node.getAttribute(null, childName);
            if (attribute != null) {
                childSet.addElement(attribute.getRef(useReferenceCache));
            }
        }
        return childSet;
    }

    /**
     * Create a copy of the evaluation context, with a new context ref.
     *
     * When determining what the original reference field of the new object
     * should be:
     * - Use the 'original' field from the original object.
     * - If it is unset, use the original objects context reference.
     * - If that is '/' then use the new context reference
     *
     * @param newContextRef      the new context anchor reference
     * @param newContextPosition the new position of the context (in a repeat
     *                           group)
     * @param subContext         the new query context for optimization, may differ from this
     *                           context if there has been a drastic change in query scope
     * @return a copy of this evaluation context, with a new context reference
     * set and the original context reference correspondingly updated.
     */
    public EvaluationContext rescope(TreeReference newContextRef, int newContextPosition,
                                     QueryContext subContext) {
        EvaluationContext ec = new EvaluationContext(this, newContextRef);
        ec.setQueryContext(subContext);
        ec.currentContextPosition = newContextPosition;

        TreeReference originalContextRef = this.getOriginalContextForPropogation();
        if (originalContextRef == null) {
            originalContextRef = newContextRef;
        }
        ec.setOriginalContext(originalContextRef);

        return ec;
    }

    /**
     * @return An evaluation context that should be used by a derived context as the original
     * context, if one exists. If one does not exist, returns null;
     */
    private TreeReference getOriginalContextForPropogation() {
        // If we have an original context reference, use it
        if (this.original != null) {
            return this.getOriginalContext();
        } else {
            // Otherwise, if the old context reference isn't '/', use that.If
            // the context ref is '/', use the new context ref as the original
            if (!TreeReference.rootRef().equals(this.getContextRef())) {
                return this.getContextRef();
            } else {
                // Otherwise propagate the original context reference field
                // with the new context reference argument
                return null;
            }
        }
    }

    public List<String> getInstanceIds() {
        return new ArrayList<>(formInstances.keySet());
    }

    public DataInstance getMainInstance() {
        return instance;
    }

    public AbstractTreeElement resolveReference(TreeReference qualifiedRef) {
        if (Thread.interrupted()) {
            throw new RequestAbandonedException();
        }
        DataInstance instance = this.getMainInstance();
        if (qualifiedRef.getInstanceName() != null &&
                (instance == null || instance.getInstanceId() == null || !instance.getInstanceId().equals(qualifiedRef.getInstanceName()))) {
            instance = this.getInstance(qualifiedRef.getInstanceName());
        }
        if (instance == null) {
            XPathMissingInstanceException e = new XPathMissingInstanceException(qualifiedRef);
            Logger.exception(e.getMessage(), e);
            throw e;
        }
        return instance.resolveReference(qualifiedRef, this);
    }

    /**
     * The context's current position in terms the nodes available for the
     * context's path. I.e. if the context points to the 3rd node that /a/b/c
     * resolves to, then the current position is 3.
     */
    public int getContextPosition() {
        return currentContextPosition;
    }

    /**
     * Get the relevant cache host for the provided ref, if one exists.
     */
    public CacheHost getCacheHost(TreeReference ref) {
        DataInstance instance = retrieveInstance(ref);
        if (instance == null) {
            return null;
        }
        return instance.getCacheHost();
    }

    /**
     * Get the instance of the reference argument, if it's present in this
     * context's form instances. Otherwise returns the main instance of this
     * evaluation context.
     *
     * @param ref retreive the instance of this reference, if loaded in the
     *            context
     * @return the instance that the reference argument names, if loaded,
     * otherwise the main instance if present.
     */
    private DataInstance retrieveInstance(TreeReference ref) {
        if (ref.getInstanceName() != null &&
                formInstances.containsKey(ref.getInstanceName())) {
            return formInstances.get(ref.getInstanceName());
        } else if (instance != null) {
            return instance;
        }

        throw new RuntimeException("Unable to expand reference " +
                ref.toString(true) +
                ", no appropriate instance in evaluation context");
    }


    /**
     * Creates a record that an expression is about to be evaluated.
     *
     * @param xPathExpression the expression being evaluated
     */
    public void openTrace(XPathExpression xPathExpression) {
        if (mAccumulateExprs) {
            String expressionString = xPathExpression.toPrettyString();
            EvaluationTrace newLevel = new EvaluationTrace(expressionString);
            openTrace(newLevel);
        }
    }


    /**
     * Creates a record that an expression is about to be evaluated.
     *
     * @param newLevel The new trace to be added to the current evaluation
     */
    public void openTrace(EvaluationTrace newLevel) {
        if (mAccumulateExprs) {
            newLevel.setParent(debugContext.currentTraceLevel);
            if (debugContext.currentTraceLevel != null) {
                debugContext.currentTraceLevel.addSubTrace(newLevel);
            }

            debugContext.currentTraceLevel = newLevel;
        }
    }

    /**
     * Creates a record that we are going to attempt to expanding a set of bulk lookup
     * predicates
     */
    private void openBulkTrace() {
        if (mAccumulateExprs) {
            BulkEvaluationTrace newLevel = new BulkEvaluationTrace();
            // We can't really track bulk traces from root contexts
            openTrace(newLevel);
        }
    }

    /**
     * Creates a record that we are going to attempt to expand a set of bulk lookup predicates
     */
    private void reportBulkTraceResults(Vector<XPathExpression> startingSet,
                                        Vector<XPathExpression> finalSet,
                                        Collection<TreeReference> childSet) {
        if (mAccumulateExprs) {
            if (!(debugContext.currentTraceLevel instanceof BulkEvaluationTrace)) {
                throw new RuntimeException("Predicate tree mismatch");
            }
            BulkEvaluationTrace trace = (BulkEvaluationTrace)debugContext.currentTraceLevel;
            trace.setEvaluatedPredicates(startingSet, finalSet, childSet);
            if (!(trace.isBulkEvaluationSucceeded())) {
                EvaluationTrace parentTrace = trace.getParent();
                if (parentTrace == null) {
                    trace.markClosed();
                    //no need to remove from the parent context if it doens't exist
                    return;
                }
                Vector<EvaluationTrace> traces = trace.getParent().getSubTraces();
                synchronized (traces) {
                    traces.remove(trace);
                }
            }
        }
    }

    public void reportSubtrace(EvaluationTrace trace) {
        if (mAccumulateExprs && debugContext.currentTraceLevel != null) {
            debugContext.currentTraceLevel.addSubTrace(trace);
        }
    }


    /**
     * Records the outcome of the current trace by value.
     *
     * @param value The result of the currently open Trace Expression
     */
    public void reportTraceValue(Object value, boolean fromCache) {
        if (mAccumulateExprs) {
            // Lazy nodeset evaluation makes it impossible for the trace to
            // record predicate subexpressions properly, so trigger that
            // evaluation now.
            if (value instanceof XPathLazyNodeset) {
                ((XPathLazyNodeset)value).size();
            }
            debugContext.currentTraceLevel.setOutcome(value, fromCache);
        }
    }


    /**
     * Closes the current evaluation trace and records the
     * relevant outcomes and context
     */
    public void closeTrace() {
        if (mAccumulateExprs) {
            if (debugContext.traceReporter != null &&
                    (debugContext.currentTraceLevel.getParent() == null || debugContext.traceReporter.reportAsFlat())) {
                debugContext.traceReporter.reportTrace(debugContext.currentTraceLevel);
            }

            if (debugContext.currentTraceLevel.getParent() == null) {
                debugContext.traceRoot = debugContext.currentTraceLevel;
            }

            debugContext.currentTraceLevel = debugContext.currentTraceLevel.getParent();
        }
    }

    /**
     * Sets this EC to be the base of a trace capture for debugging.
     */
    public void setDebugModeOn() {
        setDebugModeOn(null);
    }

    /**
     * Sets this EC to be the base of a trace capture for debugging.
     */
    public void setDebugModeOn(EvaluationTraceReporter reporter) {
        this.mAccumulateExprs = true;
        this.debugContext = this;
        this.traceReporter = reporter;
    }


    /**
     * @return the trace of the expression evaluation that was performed
     * against this context.
     */
    public EvaluationTrace getEvaluationTrace() {
        return traceRoot;
    }

    /**
     * Spawn a new evaluation context with the same context information as this context
     * but which can maintain it's own lifecycle, including a fresh query context and
     * capacity to abandon requests
     */
    public EvaluationContext spawnWithCleanLifecycle() {
        return spawnWithCleanLifecycle(null);
    }

    public EvaluationContext spawnWithCleanLifecycle(Map<String, ExternalDataInstance> additionalInstances) {
        EvaluationContext ec = new EvaluationContext(this, this.getContextRef());
        QueryContext qc = ec.getCurrentQueryContext().forceNewChildContext();
        ec.setQueryContext(qc);
        if (additionalInstances != null) {
            ec.updateInstances(additionalInstances);
        }
        return ec;
    }

    private void updateInstances(Map<String, ExternalDataInstance> instances) {
        Multimap<String, ExternalDataInstance> byRef = getInstancesByRef();
        instances.forEach((name, instance) -> {
            String ref = instance.getReference();
            if (!byRef.containsKey(ref)) {
                if (formInstances.containsKey(name)) {
                    throw new RuntimeException(
                            String.format("EvaluationContext already contains an instance with "
                                    + "ID %s with a different ref", name));
                }
                formInstances.put(name, instance);
            } else {
                for (ExternalDataInstance existing : byRef.get(ref)) {
                    if (existing.getRoot() == null) {
                        // just in time initializing of the instance
                        String instanceId = existing.getInstanceId();
                        TreeElement root = (TreeElement)instance.getRoot();
                        if (instanceId != name) {
                            root = TreeUtilities.renameInstance(root, instanceId);
                        }
                        root.setParent(existing.getBase());
                        existing.copyFromSource(new ConcreteInstanceRoot(root));
                    }
                }
            }
            if (!formInstances.containsKey(name) || formInstances.get(name).getRoot() == null) {
                // instance name is the same so no need to rename it
                formInstances.put(name, instance);
            }
        });
    }

    private Multimap<String, ExternalDataInstance> getInstancesByRef() {
        ImmutableListMultimap.Builder<String, ExternalDataInstance> builder = ImmutableListMultimap.builder();
        formInstances.values().forEach((instance) -> {
            if (instance instanceof ExternalDataInstance) {
                ExternalDataInstance di = (ExternalDataInstance)instance;
                builder.put(di.getReference(), di);
            }
        });
        return builder.build();
    }

    public void setUseReferenceCache(boolean useReferenceCache) {
        this.useReferenceCache = useReferenceCache;
    }
}
