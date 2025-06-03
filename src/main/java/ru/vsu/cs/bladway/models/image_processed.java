package ru.vsu.cs.bladway.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.vsu.cs.bladway.enums.center_init_method;
import ru.vsu.cs.bladway.enums.segmentation_method;

import java.util.List;

@Entity
@NoArgsConstructor
@Data
public class image_processed {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "image_processed_id")
    private long imageProcessedId;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_processed_labels", nullable = false)
    private byte[] imageProcessedLabels;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_processed_segments", nullable = false)
    private byte[] imageProcessedSegments;
    @Column(name = "k_value", nullable = false)
    private int kValue;
    @Column(name = "iteration_count", nullable = false)
    private int iterationCount;
    @Enumerated(EnumType.STRING)
    @Column(name = "center_init_method", nullable = false)
    private center_init_method centerInitMethod;
    @Enumerated(EnumType.STRING)
    @Column(name = "segmentation_method", nullable = false)
    private segmentation_method segmentationMethod;
    @ManyToOne
    @JoinColumn(name = "image_id", nullable = false)
    private image originalImage;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "error", joinColumns = @JoinColumn(name = "image_processed_id"))
    @OrderColumn(name = "index")
    @Column(name = "image_iteration_errors", nullable = false)
    private List<Double> imageIterationErrors;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "time", joinColumns = @JoinColumn(name = "image_processed_id"))
    @OrderColumn(name = "index")
    @Column(name = "image_processing_times", nullable = false)
    private List<Long> imageProcessingTimes;

    public image_processed(
            byte[] imageProcessedLabels,
            byte[] imageProcessedSegments,
            Integer k_value,
            Integer iteration_count,
            center_init_method center_init_method,
            segmentation_method segmentation_method,
            image original_image,
            List<Double> image_iteration_errors,
            List<Long> image_processing_times
    ) {
        this.imageProcessedLabels = imageProcessedLabels;
        this.imageProcessedSegments = imageProcessedSegments;
        this.kValue = k_value;
        this.iterationCount = iteration_count;
        this.centerInitMethod = center_init_method;
        this.segmentationMethod = segmentation_method;
        this.originalImage = original_image;
        this.imageIterationErrors = image_iteration_errors;
        this.imageProcessingTimes = image_processing_times;
    }
}
