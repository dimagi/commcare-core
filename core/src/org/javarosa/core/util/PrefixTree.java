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

package org.javarosa.core.util;

import java.util.Enumeration;
import java.util.Vector;

public class PrefixTree {
	private PrefixTreeNode root;
	
	int minimumPrefixLength;
	int minimumHeuristicLength;
	
	//Common delimeters which we'd prefer as prefix breaks rather than
	//maximum string space
	private static final char[] delimiters = {'\\', '/', '.'};
	private static final int delSacrifice = 3;
	boolean finalized = false;

	public PrefixTree () {
		this(0);
	}

	public PrefixTree (int minimumPrefixLength) {
		root = new PrefixTreeNode("");
		this.minimumPrefixLength = Math.max(minimumPrefixLength++, 0);
		this.minimumHeuristicLength = Math.max((int)(minimumPrefixLength / 2), 3);
	}
	
	public static int sharedPrefixLength (String a, String b) {
		int len;
		
		for (len = 0; len < a.length() && len < b.length(); len++) {
			if (a.charAt(len) != b.charAt(len))
				break;
		}
		
		return len;
	}
	
	public PrefixTreeNode addString (String s) {
		if(finalized) { 
			throw new RuntimeException("Can't manipulate a finalized Prefix Tree");
		}
		PrefixTreeNode current = root;

		while (s.length() > 0) {
			
			//The length of the string we've incorporated into the tree
			int len = 0;
			
			//The (potential) next node in the tree which prefixes the rest of the string
			PrefixTreeNode node = null;

			if (current.getChildren() != null) {
				for (Enumeration e = current.getChildren().elements(); e.hasMoreElements(); ) {
					node = (PrefixTreeNode)e.nextElement();
					
					String prefix = node.getPrefix();
					if(prefix.equals(s)) {
						return node;
					}
					
					len = sharedPrefixLength(s, prefix);
					if (len > minimumPrefixLength) {
						//See if we have any breaks which might make more heuristic sense than simply grabbing the biggest
						//difference
						for(char c : delimiters) {
							int sepLen = prefix.lastIndexOf(c, len - 1) + 1;
							if(sepLen != -1 && len - sepLen < delSacrifice && sepLen > minimumHeuristicLength) {
								len = sepLen;
							}
						}
						
						break; 
					}
					node = null;
				}
			}
				
			//If we didn't find anything that shared any common roots
			if (node == null) {
				//Create a placeholder for the rest of the string
				node = new PrefixTreeNode(s);
				
				//Note that we're accounting for the remainder
				len = s.length();
								
				//Add this to the highest level prefix we've found
				current.addChild(node);
			} else if (len < node.getPrefix().length()) {
				String prefix = s.substring(0, len);
				
				PrefixTreeNode interimNode = current.budChild(node, prefix, len);
				
				node = interimNode;
			}
			
			current = node;
			s = s.substring(len);
		}
		
		current.setTerminal();
		return current;
	}
	
	public Vector<String> getStrings () {
		if(finalized) { 
			throw new RuntimeException("Can't get the strings from a finalized Prefix Tree");
		}
		Vector<String> v = new Vector<String>();
		root.decompose(v, "");
		return v;
	}
	
	public String toString() {
		return root.toString();
	}
	public void seal() {
		//System.out.println(toString());
		root.seal();
		finalized = true;
	}
	
	public void clear() {
		finalized = false;
		root = new PrefixTreeNode("");
	}
}
