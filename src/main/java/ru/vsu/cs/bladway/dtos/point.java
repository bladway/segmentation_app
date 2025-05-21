package ru.vsu.cs.bladway.dtos;

public class point {
    public int x;
    public int y;

    public point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "{" + x + ", " + y + "}";
    }
}