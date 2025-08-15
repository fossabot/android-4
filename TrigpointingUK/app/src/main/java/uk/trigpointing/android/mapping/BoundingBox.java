package uk.trigpointing.android.mapping;

/**
 * Simple BoundingBox class to replace OSMdroid dependency.
 * Represents a geographic bounding box with north, south, east, west coordinates.
 */
public class BoundingBox {
    private final double latNorth;
    private final double latSouth;
    private final double lonEast;
    private final double lonWest;

    public BoundingBox(double north, double east, double south, double west) {
        this.latNorth = north;
        this.latSouth = south;
        this.lonEast = east;
        this.lonWest = west;
    }

    public double getLatNorth() {
        return latNorth;
    }

    public double getLatSouth() {
        return latSouth;
    }

    public double getLonEast() {
        return lonEast;
    }

    public double getLonWest() {
        return lonWest;
    }

    @Override
    public String toString() {
        return "BoundingBox{" +
                "north=" + latNorth +
                ", south=" + latSouth +
                ", east=" + lonEast +
                ", west=" + lonWest +
                '}';
    }
}
