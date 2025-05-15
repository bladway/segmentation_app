package ru.vsu.cs.bladway.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@NoArgsConstructor
@Data
public class image {
    public image(byte[] image_raw, byte[] markup_raw, int height, int width) {
        this.image_raw = image_raw;
        this.markup_raw = markup_raw;
        this.image_height = height;
        this.image_width = width;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long image_id;

    @Lob
    @Column(nullable = false)
    private byte[] image_raw;

    @Lob
    @Column
    private byte[] markup_raw;

    @Column(nullable = false)
    private int image_height;

    @Column(nullable = false)
    private int image_width;

    @OneToMany(mappedBy = "original_image", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<image_processed> images_processed;
}
