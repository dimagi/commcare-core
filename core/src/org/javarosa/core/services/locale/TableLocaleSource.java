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

/**
 * 
 */
package org.javarosa.core.services.locale;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.UnregisteredLocaleException;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author Clayton Sims
 * @date May 26, 2009 
 *
 */
public class TableLocaleSource implements LocaleDataSource {
	private WeakReference dataCache; /*{ String -> String } */
	private byte[] data;
	private OrderedHashtable<String, String> editing;
	boolean currentlyEditing = false;
	
	public TableLocaleSource() {
	}
	
	public TableLocaleSource(OrderedHashtable<String, String> localeData) {
		this.data = this.toArray(localeData);
		if(this.data == null) {
			throw new RuntimeException("Huge problem when attempting to init locale data source!");
		}
	}
	
	public void startEditing() {
		if(data == null) {
			editing = new OrderedHashtable<String, String>();
			dataCache = new WeakReference(editing);
		} else {
			editing = getLocalizedText();
		}
		currentlyEditing = true;
		//clear our store
		data = null;
	}
	/**
	 * Set a text mapping for a single text handle for a given locale.
	 * 
	 * @param textID Text handle. Must not be null. Need not be previously defined for this locale.
	 * @param text Localized text for this text handle and locale. Will overwrite any previous mapping, if one existed.
	 * If null, will remove any previous mapping for this text handle, if one existed.
	 * @throws UnregisteredLocaleException If locale is not defined or null.
	 * @throws NullPointerException if textID is null
	 */
	public synchronized void setLocaleMapping (String textID, String text) {
		if(!currentlyEditing) {
			throw new RuntimeException("attempt to modify locale table source without entering edit mode!");
		}
		if(textID == null) {
			throw new NullPointerException("Null textID when attempting to register " + text + " in locale table");
		}
		if (text == null) {
			editing.remove(textID);
		} else {
			editing.put(textID, text);
			
		}
	}
	
	public synchronized void stopEditing() {
		data = toArray(editing);
		dataCache = new WeakReference(editing);
		editing = null;
		currentlyEditing = false;
	}
	
	/**
	 * Determine whether a locale has a mapping for a given text handle. Only tests the specified locale and form; does
	 * not fallback to any default locale or text form.
	 * 
	 * @param textID Text handle.
	 * @return True if a mapping exists for the text handle in the given locale.
	 * @throws UnregisteredLocaleException If locale is not defined.
	 */
	public synchronized boolean hasMapping (String textID) {
		return (textID == null ? false : getLocalizedText().get(textID) != null);
	}
	
	
	public synchronized boolean equals(Object o) {
		if(!(o instanceof TableLocaleSource)) {
			return false;
		}
		TableLocaleSource l = (TableLocaleSource)o;
		return ExtUtil.equals(getLocalizedText(), l.getLocalizedText());
	}

	public synchronized OrderedHashtable<String, String> getLocalizedText() {
		OrderedHashtable<String, String> retData = dataCache == null ? null : (OrderedHashtable<String, String>)dataCache.get();
		if(retData == null) {
			DataInputStream dis = null;
			try {
				dis = new DataInputStream(new ByteArrayInputStream(data));
				retData =(OrderedHashtable)ExtUtil.read(dis, new ExtWrapMap(String.class, String.class, true), ExtUtil.defaultPrototypes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DeserializationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally { 
				if(dis != null) {
					try {
						dis.close();
					} catch(IOException e) {
						//stupid
					}
				}
			}
		}
		dataCache = new WeakReference(retData);
		return retData;
	}
	
	private byte[] toArray(OrderedHashtable<String, String> data) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ExtUtil.write(new DataOutputStream(bos), new ExtWrapMap(getLocalizedText()));
			return bos.toByteArray();
		} catch (IOException e) {
			//hmm....
			return null;
		} finally { 
			if(bos != null) {
				try {
					bos.close();
				} catch(IOException e) {
					//stupid
				}
			}
		}
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		data = ExtUtil.readBytes(in);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		if(currentlyEditing) {
			throw new IllegalStateException("table locale source was never finalized before serialization!");
		}
		ExtUtil.writeBytes(out, data);
	}
}
