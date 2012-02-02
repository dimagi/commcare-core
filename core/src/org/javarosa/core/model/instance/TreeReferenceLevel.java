/**
 * 
 */
package org.javarosa.core.model.instance;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

/**
 * @author ctsims
 *
 */
public class TreeReferenceLevel implements Externalizable {
	public static final int MULT_UNINIT = -16;
	
	private String name;
	private int multiplicity = MULT_UNINIT;
	private Vector<XPathExpression> predicates;
	
	public TreeReferenceLevel() {
		
	}
	
	
	public TreeReferenceLevel(String name, int multiplicity, Vector<XPathExpression> predicates) {
		this.name = name.intern();
		this.multiplicity = multiplicity;
		this.predicates = predicates;
	}

	public TreeReferenceLevel(String name, int multiplicity) {
		this(name, multiplicity, null);
	}


	public int getMultiplicity() {
		return multiplicity;
	}

	public String getName() {
		return name;
	}

	public TreeReferenceLevel setMultiplicity(int mult) {
		return new TreeReferenceLevel(name, mult, predicates);
	}

	public TreeReferenceLevel setPredicates(Vector<XPathExpression> xpe) {
		return new TreeReferenceLevel(name, multiplicity, xpe);
	}

	public Vector<XPathExpression> getPredicates() {
		return this.predicates;
	}
	
	public TreeReferenceLevel shallowCopy() {
		return new TreeReferenceLevel(name, multiplicity, predicates);
	}


	public TreeReferenceLevel setName(String name) {
		return new TreeReferenceLevel(name, multiplicity, predicates);
	}


	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		name = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		multiplicity = ExtUtil.readInt(in);
		predicates = ExtUtil.nullIfEmpty((Vector<XPathExpression>)ExtUtil.read(in,new ExtWrapList(new ExtWrapTagged())));
	}


	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(name));
		ExtUtil.writeNumeric(out, multiplicity);
		ExtUtil.write(out, new ExtWrapList(ExtUtil.emptyIfNull(predicates)));
	}
}
