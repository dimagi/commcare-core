/**
 * 
 */
package org.commcare.reference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ctsims
 *
 */
public class FileSytemResource implements Reference {
	
	String raw;
	String URI;
	
	public FileSytemResource(String URI) {
		this(URI,URI);
	}
	
	public FileSytemResource(String raw, String URI) {
		this.raw = raw;
		this.URI = URI;
	}
	
	public boolean doesBinaryExist() {
		InputStream is;
		try {
			String fname= "C:\\eclipse_workspaces\\JavaRosa\\commcare\\application\\resources" + URI.replace('/', File.separatorChar);
			System.out.println(fname);
			is = new FileInputStream("C:\\eclipse_workspaces\\JavaRosa\\commcare\\application\\resources" + URI.replace('/', File.separatorChar));
			is.close();
		} catch (FileNotFoundException e1) {
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	public InputStream getStream() {
		try {
			String fname= "C:\\eclipse_workspaces\\JavaRosa\\commcare\\application\\resources" + URI.replace('/', File.separatorChar);
			System.out.println(fname);
			return new FileInputStream("C:\\eclipse_workspaces\\JavaRosa\\commcare\\application\\resources" + URI.replace('/', File.separatorChar));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getRaw() {
		return raw;
	}
	
	public String getURI() {
		return "jr://" + "file" + "//" + this.URI;
	}
	
	public Reference contextualize(String raw) {
		return new FileSytemResource(raw, ReferenceUtil.contextualizeURI(this.URI, raw));
	}

	public boolean isReadOnly() {
		return true;
	}
	
	public boolean equals(Object o) {
		if(o instanceof FileSytemResource) {
			return URI.equals(((FileSytemResource)o).URI);
		} else {
			return false;
		}
	}
}
