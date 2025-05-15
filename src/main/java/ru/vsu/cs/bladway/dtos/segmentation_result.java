package ru.vsu.cs.bladway.dtos;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.opencv.core.Mat;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class segmentation_result {
    public Mat centers_labels;
    public List<pixel> centers;
    public List<Double> iteration_errors;
    public List<Long> processing_times;
}
