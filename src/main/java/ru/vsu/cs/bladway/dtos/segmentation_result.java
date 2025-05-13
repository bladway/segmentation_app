package ru.vsu.cs.bladway.dtos;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.opencv.core.Mat;
import ru.vsu.cs.bladway.enums.center_init_method;
import ru.vsu.cs.bladway.enums.segmentation_method;

@NoArgsConstructor
@AllArgsConstructor
public class segmentation_result {
    public Mat centers_labels;
}
