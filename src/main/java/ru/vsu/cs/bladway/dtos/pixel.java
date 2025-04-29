package ru.vsu.cs.bladway.dtos;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.opencv.core.Point3;

@AllArgsConstructor
@NoArgsConstructor
public class pixel {
    public pixel(Integer y, Integer x, double[] bgr) {
        this.y = y;
        this.x = x;
        this.b = bgr[0];
        this.g = bgr[1];
        this.r = bgr[2];
    }

    public pixel(Integer y, Integer x, Point3 bgr) {
        this.y = y;
        this.x = x;
        this.b = bgr.x;
        this.g = bgr.y;
        this.r = bgr.z;
    }
    public Integer y;
    public Integer x;
    public Double b;
    public Double g;
    public Double r;

}
