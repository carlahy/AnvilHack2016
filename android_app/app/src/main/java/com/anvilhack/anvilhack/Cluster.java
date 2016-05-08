package com.anvilhack.anvilhack;

import java.util.ArrayList;

/**
 * Created by jacek on 08/05/16.
 */
public class Cluster {
    private Point centroid;
    private ArrayList<Point> points;

    public Cluster(Point centroid) {
        this.centroid = centroid;
        this.points = new ArrayList<>();
    }

    public Cluster(Point centroid, ArrayList<Point> points) {
        this.centroid = centroid;
        this.points = points;
    }

    public double distFromCentre(Point p) {
        double x1 = p.getX();
        double y1 = p.getY();
        double x2 = centroid.getX();
        double y2 = centroid.getY();
        return (x1-x2)*(x1-x2) + (y1-y2) * (y1-y2);
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    public Point getCentroid() {
        return centroid;
    }

    public ArrayList<Point> getPoints() {
        return points;
    }

    public int size() {
        return 1 + points.size();
    }
}
