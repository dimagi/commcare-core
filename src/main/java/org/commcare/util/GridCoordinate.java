package org.commcare.util;

/**
 * @author wspride
 *         Represents a rectangle in a Detail's EntityViewTile, via the coordinate of the top
 *         left corner (gridX and gridY) and the height down (gridHeight) and
 *         width right (gridWidth) from there.
 */

public class GridCoordinate {
    private final int gridX;
    private final int gridY;
    private final int gridWidth;
    private final int gridHeight;

    public GridCoordinate(int x, int y, int w, int h) {
        gridX = x;
        gridY = y;
        gridWidth = w;
        gridHeight = h;
    }

    public int getX() {
        return gridX;
    }

    public int getY() {
        return gridY;
    }

    public int getWidth() {
        return gridWidth;
    }

    public int getHeight() {
        return gridHeight;
    }

    public String toString() {
        return "x: " + gridX + ", y: " + gridY + ", gridWidth: " + gridWidth + ", gridHeight: " + gridHeight;
    }

}
