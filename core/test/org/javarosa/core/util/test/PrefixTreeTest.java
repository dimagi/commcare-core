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

package org.javarosa.core.util.test;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

import java.util.Vector;

import org.javarosa.core.model.test.QuestionDefTest;
import org.javarosa.core.util.PrefixTree;
import org.javarosa.core.util.PrefixTreeNode;

public class PrefixTreeTest extends TestCase  {
	public PrefixTreeTest(String name, TestMethod rTestMethod) {
		super(name, rTestMethod);
	}

	public PrefixTreeTest(String name) {
		super(name);
	}

	public PrefixTreeTest() {
		super();
	}	

	public Test suite() {
		TestSuite aSuite = new TestSuite();
		System.out.println("Running PrefixTreeTests...");
		for (int i = 1; i <= NUM_TESTS; i++) {
			final int testID = i;
			aSuite.addTest(new PrefixTreeTest("PrefixTree Test " + i, new TestMethod() {
				public void run (TestCase tc) {
					((PrefixTreeTest)tc).doTest(testID);
				}
			}));
		}
			
		return aSuite;
	}
	

	private int[] prefixLengths = new int[] {0, 1, 2, 10, 50};
	
	public final static int NUM_TESTS = 2;
	public void doTest (int i) {
		switch (i) {
		case 1: testBasic(); break;
		case 2: testHeuristic(); break;
		}
	}


	public void add (PrefixTree t, String s) {
		PrefixTreeNode  node = t.addString(s);
		//System.out.println(t.toString());
		
		if(!node.render().equals(s)) {
			fail("Prefix tree mangled: " + s + " into " + node.render());
		}
		
		Vector v = t.getStrings();
		for (int i = 0; i < v.size(); i++) {
			//System.out.println((String)v.elementAt(i));
		}
	}
	
	public void testBasic() {
		for(int i : prefixLengths) {
			PrefixTree t = new PrefixTree(i);	
			
			add(t, "abcde");
			add(t, "abcdefghij");
			add(t, "abcdefghijklmno");
			add(t, "abcde");
			add(t, "abcdefg");
			add(t, "xyz");
			add(t, "abcdexyz");
			add(t, "abcppppp");
			System.out.println(t.toString());
		}
		
	}
	
	public void testHeuristic() {
		for(int i : prefixLengths) {

		PrefixTree t = new PrefixTree(i);
	
		add(t, "jr://file/images/something/abcd.png");
		add(t, "jr://file/audio/something/abcd.mp3");
		add(t, "jr://file/audio/something/adfd.mp3");
		add(t, "jr://file/images/something/dsf.png");
		add(t, "jr://file/images/sooth/abcd.png");
		add(t, "jr://file/audio/something/bsadf.mp3");
		add(t, "jr://file/audio/something/fsde.mp3");
			
		add(t, "jr://file/images/some");
		System.out.println(t.toString());
		}
	}
}
