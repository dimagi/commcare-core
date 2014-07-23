package org.commcare.util;

public class GridCoordinate {
	private int gridX;
	private int gridY;
	private int gridWidth;
	private int gridHeight;
	
	public GridCoordinate(int x, int y, int w, int h){
		gridX = x;
		gridY = y;
		gridWidth = w;
		gridHeight = h;
	}
	
	public int getX(){
		return gridX;
	}
	
	public int getY(){
		return gridY;
	}
	
	public int getWidth(){
		return gridWidth;
	}
	
	public int getHeight(){
		return gridHeight;
	}
	
	public String toString(){
		return "x: " + gridX + ", y: " + gridY + ", gridWidth: " + gridWidth + ", gridHeight: " + gridHeight;
	}
	
}
