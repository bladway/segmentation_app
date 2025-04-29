package ru.vsu.cs.bladway.dtos;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.opencv.core.Mat;

@NoArgsConstructor
@AllArgsConstructor
public class segmentation_result {
    public Mat input_image;
    public Mat output_image;
    public Mat centers_labels;
    public int K;
    public double epsilon;
}
