package ru.vsu.cs.bladway.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@NoArgsConstructor
@Data
public class image {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "image_id")
    private long imageId;
    @Lob
    @Column(name = "image_raw", nullable = false)
    private byte[] imageRaw;
    @Lob
    @Column(name = "markup_raw")
    private byte[] markupRaw;
    @Column(name = "image_height", nullable = false)
    private int imageHeight;
    @Column(name = "image_width", nullable = false)
    private int imageWidth;
    @OneToMany(mappedBy = "originalImage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<image_processed> imagesProcessed;

    public image(byte[] image_raw, byte[] markup_raw, int height, int width) {
        this.imageRaw = image_raw;
        this.markupRaw = markup_raw;
        this.imageHeight = height;
        this.imageWidth = width;
    }
}
