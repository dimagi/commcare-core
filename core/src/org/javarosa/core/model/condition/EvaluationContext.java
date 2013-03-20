/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.core.model.condition;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.IExprDataType;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;

/* a collection of objects that affect the evaluation of an expression, like function handlers
 * and (not supported) variable bindings
 */
public class EvaluationContext {
	private TreeReference contextNode; //unambiguous ref used as the anchor for relative paths
	private Hashtable functionHandlers;
	private Hashtable variables;
	
	public boolean isConstraint; //true if we are evaluating a constraint
	public IAnswerData candidateValue; //if isConstraint, this is the value being validated
	public boolean isCheckAddChild; //if isConstraint, true if we are checking the constraint of a parent node on how
									//  many children it may have
	
	private String outputTextForm = null; //Responsible for informing itext what form is requested if relevant
	
	private Hashtable<String, DataInstance> formInstances;
	
	private TreeReference original;
	private int currentContextPosition = -1;
	
	DataInstance instance;
	int[] predicateEvaluationProgress;
	
	/** Copy Constructor **/
	private EvaluationContext (EvaluationContext base) {
		//TODO: These should be deep, not shallow
		this.functionHandlers = base.functionHandlers;
		this.formInstances = base.formInstances;
		this.variables = base.variables;
		
		this.contextNode = base.contextNode;
		this.instance = base.instance;
		
		this.isConstraint = base.isConstraint;
		this.candidateValue = base.candidateValue;
		this.isCheckAddChild = base.isCheckAddChild;
		
		this.outputTextForm = base.outputTextForm;
		this.original = base.original;
		
		//Hrm....... not sure about this one. this only happens after a rescoping,
		//and is fixed on the context. Anything that changes the context should
		//invalidate this
		this.currentContextPosition = base.currentContextPosition;
	}
	
	public EvaluationContext (EvaluationContext base, TreeReference context) {
		this(base);
		this.contextNode = context;
	}
	
	public EvaluationContext (EvaluationContext base, Hashtable<String, DataInstance> formInstances, TreeReference context) {
		this(base, context);
		this.formInstances = formInstances;
	}
	
	public EvaluationContext (FormInstance instance, Hashtable<String, DataInstance> formInstances, EvaluationContext base) {
		this(base);
		this.formInstances = formInstances;
		this.instance = instance;
	}

	public EvaluationContext (DataInstance instance) {
		this(instance, new Hashtable<String, DataInstance>());
	}
	
	public EvaluationContext (DataInstance instance, Hashtable<String, DataInstance> formInstances) {
		this.formInstances = formInstances; 
		this.instance = instance;
		this.contextNode = TreeReference.rootRef();
		functionHandlers = new Hashtable();
		variables = new Hashtable();
	}
	
	public DataInstance getInstance(String id) {
		return formInstances.containsKey(id) ? formInstances.get(id) : null;
	}
	
	public TreeReference getContextRef () {
		return contextNode;
	}
	
	public void setOriginalContext(TreeReference ref) {
		this.original = ref;
	}
	
	public TreeReference getOriginalContext() {
		if(this.original == null) { return this.contextNode;}
		else { return this.original; }
	}
	
	public void addFunctionHandler (IFunctionHandler fh) {
		functionHandlers.put(fh.getName(), fh);
	}
	
	public Hashtable getFunctionHandlers () {
		return functionHandlers;
	}
	
	public void setOutputTextForm(String form) {
		this.outputTextForm = form;
	}
	
	public String getOutputTextForm() {
		return outputTextForm;
	}
	
	public void setVariables(Hashtable<String, ?> variables) {
		for (Enumeration e = variables.keys(); e.hasMoreElements(); ) {
			String var = (String)e.nextElement();
			setVariable(var, variables.get(var));
		}
	}
	
	public void setVariable(String name, Object value) {
		//No such thing as a null xpath variable. Empty
		//values in XPath just get converted to ""
		if(value == null) {
			variables.put(name, "");
			return;
		}
		//Otherwise check whether the value is one of the normal first
		//order datatypes used in xpath evaluation
		if(value instanceof Boolean ||
				   value instanceof Double  ||
				   value instanceof String  ||
				   value instanceof Date    ||
				   value instanceof IExprDataType) {
				variables.put(name, value);
				return;
		}
		
		//Some datatypes can be trivially converted to a first order
		//xpath datatype
		if(value instanceof Integer) {
			variables.put(name, new Double(((Integer)value).doubleValue()));
			return;
		}
		if(value instanceof Float) {
			variables.put(name, new Double(((Float)value).doubleValue()));
			return;
		}
		
		//Otherwise we just hope for the best, I suppose? Should we log this?
		else {
			variables.put(name, value);
		}
	}
	
	public Object getVariable(String name) {
		return variables.get(name);
	}
	
	public Vector<TreeReference> expandReference(TreeReference ref) {
		return expandReference(ref, false);
	}

	// take in a potentially-ambiguous ref, and return a vector of refs for all nodes that match the passed-in ref
	// meaning, search out all repeated nodes that match the pattern of the passed-in ref
	// every ref in the returned vector will be unambiguous (no index will ever be INDEX_UNBOUND)
	// does not return template nodes when matching INDEX_UNBOUND, but will match templates when INDEX_TEMPLATE is explicitly set
	// return null if ref is relative, otherwise return vector of refs (but vector will be empty is no refs match)
	// '/' returns {'/'}
	// can handle sub-repetitions (e.g., {/a[1]/b[1], /a[1]/b[2], /a[2]/b[1]})
	public Vector<TreeReference> expandReference(TreeReference ref, boolean includeTemplates) {
		if (!ref.isAbsolute()) {
			return null;
		}
		
		AbstractTreeElement base;
		DataInstance baseInstance;
		if(ref.getInstanceName() != null && formInstances.containsKey(ref.getInstanceName())) {
			baseInstance = formInstances.get(ref.getInstanceName());
		} else if(instance != null) {
			baseInstance = instance;
		} else {
			throw new RuntimeException("Unable to expand reference " + ref.toString(true) + ", no appropriate instance in evaluation context");
		}

		Vector<TreeReference> v = new Vector<TreeReference>();
		expandReference(ref,baseInstance, baseInstance.getRoot().getRef(), v, includeTemplates);
		return v;
	}

	// recursive helper function for expandReference
	// sourceRef: original path we're matching against
	// node: current node that has matched the sourceRef thus far
	// workingRef: explicit path that refers to the current node
	// refs: Vector to collect matching paths; if 'node' is a target node that
	// matches sourceRef, templateRef is added to refs
	private void expandReference(TreeReference sourceRef, DataInstance instance, TreeReference workingRef, Vector<TreeReference> refs, boolean includeTemplates) {
		int depth = workingRef.size();
		Vector<XPathExpression> predicates = null;
		
		//check to see if we've matched fully
		if (depth == sourceRef.size()) {
			//TODO: Do we need to clone these references?
			refs.addElement(workingRef);
		} else {
			//Otherwise, need to get the next set of matching references
			
			String name = sourceRef.getName(depth);
			predicates = sourceRef.getPredicate(depth);
			
			//Copy predicates for batch fetch
			if(predicates != null) {
				Vector<XPathExpression> predCopy = new Vector<XPathExpression>();
				for(XPathExpression xpe : predicates) {
					predCopy.addElement(xpe);
				}
				predicates = predCopy;
			}
			
			int mult = sourceRef.getMultiplicity(depth);
			Vector<TreeReference> set = new Vector<TreeReference>();
			
			AbstractTreeElement node = instance.resolveReference(workingRef);
			Vector<TreeReference> passingSet = new Vector<TreeReference>();
			
			Vector<TreeReference> children = node.tryBatchChildFetch(name, mult, predicates, this);
			
			if(children != null) {
				set = children;
			} else {
			
				if (node.hasChildren()) {
					if (mult == TreeReference.INDEX_UNBOUND) {
						int count = node.getChildMultiplicity(name);
						for (int i = 0; i < count; i++) {
							AbstractTreeElement child = node.getChild(name, i);
							if (child != null) {
								set.addElement(child.getRef());
							} else {
								throw new IllegalStateException("Missing or non-sequntial nodes expanding a reference"); // missing/non-sequential
								// nodes
							}
						}
						if (includeTemplates) {
							AbstractTreeElement template = node.getChild(name, TreeReference.INDEX_TEMPLATE);
							if (template != null) {
								set.addElement(template.getRef());
							}
						}
					} else if(mult != TreeReference.INDEX_ATTRIBUTE){
						//TODO: Make this test mult >= 0?
						//If the multiplicity is a simple integer, just get
						//the appropriate child
						AbstractTreeElement child = node.getChild(name, mult);
						if (child != null) {
							set.addElement(child.getRef());
						}
					}
				}
				
				if(mult == TreeReference.INDEX_ATTRIBUTE) {
					AbstractTreeElement attribute = node.getAttribute(null, name);
					if (attribute != null) {
						set.addElement(attribute.getRef());
					}
				}
			}

			if(predicates != null && predicateEvaluationProgress != null) {
				predicateEvaluationProgress[1] += set.size();
			}
			//Create a place to store the current position markers
			int[] positionContext = new int[predicates == null ? 0 : predicates.size()];
			
			//init all to 0
			for(int i = 0 ; i < positionContext.length ; ++ i) { positionContext[i] = 0; }
			
			for (Enumeration e = set.elements(); e.hasMoreElements();) {
				//if there are predicates then we need to see if e.nextElement meets the standard of the predicate
				TreeReference treeRef = (TreeReference)e.nextElement();				
				if(predicates != null)
				{
					boolean passedAll = true;
					int predIndex = -1;
					for(XPathExpression xpe : predicates)
					{
						predIndex++;
						//Just by getting here we're establishing a position for evaluating the current 
						//context. If we break, we won't push up the next one
						positionContext[predIndex]++;
						
						//test the predicate on the treeElement
						//EvaluationContext evalContext = new EvaluationContext(this, treeRef);
						EvaluationContext evalContext = rescope(treeRef, positionContext[predIndex]);
						Object o = xpe.eval(instance, evalContext);
						
						//There's a special case here that can't be handled by syntactic sugar.
						//If the result of a predicate expression is an Integer, we need to 
						//evaluate whether that value is equal to the current position context
						
						o = XPathFuncExpr.unpack(o);
						
						boolean passed = false;
						
						if(o instanceof Double) {
							//The spec just says "number" for when to use
							//this, so I think this is ok? It's not clear 
							//what to do with a non-integer. It's possible
							//we are not supposed to round.
							int intVal = XPathFuncExpr.toInt(o).intValue();
							passed = (intVal == positionContext[predIndex]);
						} else if(o instanceof Boolean) {
							passed = ((Boolean)o).booleanValue();
						} else {
							//???
						}
						
						if(!passed)
						{
							passedAll = false;
							break;
						}
					}
					if(predicateEvaluationProgress != null) {
						predicateEvaluationProgress[0]++;
					}
					if(passedAll)
					{
						expandReference(sourceRef, instance, treeRef, refs, includeTemplates);
					}
				}
				else
				{
					expandReference(sourceRef, instance, treeRef, refs, includeTemplates);
				}
			}
		}
	}

	private EvaluationContext rescope(TreeReference treeRef, int currentContextPosition) {
		EvaluationContext ec = new EvaluationContext(this, treeRef);
		ec.currentContextPosition = currentContextPosition;
		//If there was no original context position, we'll want to set the next original
		//context to be this rescoping (which would be the backup original one).
		if(this.original != null) {
			ec.setOriginalContext(this.getOriginalContext());
		} else {
			//Check to see if we have a context, if not, the treeRef is the original declared
			//nodeset.
			if(TreeReference.rootRef().equals(this.getContextRef())) 
			{ 
				ec.setOriginalContext(treeRef);
			} else {
				//If we do have a legit context, use it!
				ec.setOriginalContext(this.getContextRef());
			}
			
		}
		return ec;
	}

	public DataInstance getMainInstance() {
		return instance;
	}
	
	public AbstractTreeElement resolveReference(TreeReference qualifiedRef) {
		DataInstance instance = this.getMainInstance();
		if(qualifiedRef.getInstanceName() != null && (instance == null || instance.getInstanceId() != qualifiedRef.getInstanceName())) {
			instance = this.getInstance(qualifiedRef.getInstanceName());
		}
		return instance.resolveReference(qualifiedRef);
	}
	
	public int getContextPosition() {
		return currentContextPosition;
	}

	public void setPredicateProcessSet(int[] loadingDetails) {
		if(loadingDetails != null && loadingDetails.length == 2) {
			predicateEvaluationProgress = loadingDetails;
		}
	}
}
