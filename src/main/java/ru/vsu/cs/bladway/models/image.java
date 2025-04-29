package ru.vsu.cs.bladway.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@NoArgsConstructor
@Data
public class image {
    public image(byte[] image_raw) {
        this.image_raw = image_raw;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long image_id;

    @Lob
    @Column(nullable = false)
    private byte[] image_raw;


    @OneToMany(mappedBy = "original_image", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<image_processed> images_processed;
}
