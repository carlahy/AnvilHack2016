package com.anvilhack.anvilhack;

/**
 * Created by jacek on 08/05/16.
 */
public class Point {

    private Double x;
    private Double y;
    private String name;
    private String id;

    public Point(String id, String name, double x, double y) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return this.id;
    }

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
        this.name = null;
        this.id = null;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }
}
