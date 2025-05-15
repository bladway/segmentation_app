package ru.vsu.cs.bladway.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import ru.vsu.cs.bladway.enums.center_init_method;
import ru.vsu.cs.bladway.enums.segmentation_method;

import java.util.List;

@Entity
@NoArgsConstructor
@Data
public class image_processed {
    public image_processed(
            byte[] image_processed_raw,
            Integer k_value,
            Integer iteration_count,
            center_init_method center_init_method,
            segmentation_method segmentation_method,
            image original_image,
            List<Double> image_iteration_errors,
            List<Long> image_processing_times
    ) {
        this.image_processed_raw = image_processed_raw;
        this.k_value = k_value;
        this.iteration_count = iteration_count;
        this.center_init_method = center_init_method;
        this.segmentation_method = segmentation_method;
        this.original_image = original_image;
        this.image_iteration_errors = image_iteration_errors;
        this.image_processing_times = image_processing_times;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long image_processed_id;

    @Lob
    @Column(nullable = false)
    private byte[] image_processed_raw;

    @Column(nullable = false)
    private int k_value;

    @Column(nullable = false)
    private int iteration_count;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private center_init_method center_init_method;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private segmentation_method segmentation_method;

    @ManyToOne
    @JoinColumn(name = "image_id", nullable = false)
    private image original_image;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "error", joinColumns = @JoinColumn(name = "image_processed_id"))
    @OrderColumn(name = "index")
    @Column(nullable = false)
    private List<Double> image_iteration_errors;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "time", joinColumns = @JoinColumn(name = "image_processed_id"))
    @OrderColumn(name = "index")
    @Column(nullable = false)
    private List<Long> image_processing_times;
}
