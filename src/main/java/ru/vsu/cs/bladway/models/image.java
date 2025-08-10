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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "image_generator")
    @SequenceGenerator(name = "image_generator", sequenceName = "image_seq", allocationSize = 1)
    @Column(name = "image_id")
    private long imageId;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image", nullable = false)
    private byte[] image;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_markup")
    private byte[] imageMarkup;
    @Column(name = "image_height", nullable = false)
    private int imageHeight;
    @Column(name = "image_width", nullable = false)
    private int imageWidth;
    @OneToMany(mappedBy = "originalImage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<image_processed> imagesProcessed;

    public image(byte[] image, byte[] image_markup, int height, int width) {
        this.image = image;
        this.imageMarkup = image_markup;
        this.imageHeight = height;
        this.imageWidth = width;
    }
}
