package org.commcare.util;

public class GridStyle {
	private String fontSize;
	private String horzAlign;
	private String vertAlign;
	
	public GridStyle(String fs, String ha, String va){
		fontSize = fs;
		horzAlign = ha;
		vertAlign = va;
	}
	
	public String getFontSize(){
		if(fontSize == null){
			return "normal";
		}
		return fontSize;
	}
	
	public String getHorzAlign(){
		if (horzAlign == null){
			return "none";
		}
		return horzAlign;
	}
	
	public String getVertAlign(){
		if(vertAlign == null){
			return "none";
		}
		return vertAlign;
	}
	
	public String toString(){
		return "font size: " + fontSize + ", horzAlign: " + horzAlign + ", vertAlign: " + vertAlign; 
	}
	
}
