package ru.vsu.cs.bladway.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import ru.vsu.cs.bladway.enums.segmentation_method;

@Entity
@NoArgsConstructor
@Data
public class image_processed {
    public image_processed(
            byte[] image_processed_raw,
            Integer k_value,
            Integer iteration_count,
            segmentation_method segmentation_method,
            image original_image
    ) {
        this.image_processed_raw = image_processed_raw;
        this.k_value = k_value;
        this.iteration_count = iteration_count;
        this.segmentation_method = segmentation_method;
        this.original_image = original_image;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long image_processed_id;

    @Lob
    @Column(nullable = false)
    private byte[] image_processed_raw;

    @Column(nullable = false)
    private Integer k_value;

    @Column(nullable = false)
    private Integer iteration_count;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private segmentation_method segmentation_method;

    @ManyToOne
    @JoinColumn(name = "image_id", nullable = false)
    private image original_image;
}
