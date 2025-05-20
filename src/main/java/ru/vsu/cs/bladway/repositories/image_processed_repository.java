package ru.vsu.cs.bladway.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vsu.cs.bladway.enums.center_init_method;
import ru.vsu.cs.bladway.enums.segmentation_method;
import ru.vsu.cs.bladway.models.image;
import ru.vsu.cs.bladway.models.image_processed;

import java.util.List;

@Repository
public interface image_processed_repository extends JpaRepository<image_processed, Long> {
    List<image_processed> findAllBykValueAndOriginalImageAndCenterInitMethodAndSegmentationMethod(int kValue, image originalImage, center_init_method centerInitMethod, segmentation_method segmentationMethod);
}
