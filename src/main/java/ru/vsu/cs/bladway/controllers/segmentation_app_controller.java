package ru.vsu.cs.bladway.controllers;

import lombok.RequiredArgsConstructor;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.vsu.cs.bladway.enums.segmentation_method;
import ru.vsu.cs.bladway.models.image;
import ru.vsu.cs.bladway.models.image_processed;
import ru.vsu.cs.bladway.repositories.image_processed_repository;
import ru.vsu.cs.bladway.repositories.image_repository;
import ru.vsu.cs.bladway.utils.math_util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static ru.vsu.cs.bladway.segmentation_app.images_extension;

@RequiredArgsConstructor
@Controller
public class segmentation_app_controller {

    public image read_by_id_image(Long id) {
        Optional<image> if_image = image_repository.findById(id);
        return if_image.orElse(null);
    }

    public image_processed read_by_id_image_processed(Long id) {
        Optional<image_processed> if_image_processed = image_processed_repository.findById(id);
        return if_image_processed.orElse(null);
    }

    public Mat read_mat_by_image(image image) {
        byte[] image_raw = image.getImage_raw();
        if (image_raw == null) { return null; }
        return Imgcodecs.imdecode(
                new MatOfByte(image_raw),
                Imgcodecs.IMREAD_UNCHANGED
        );
    }

    public Mat read_mat_by_image_processed(image_processed image_processed) {
        byte[] image_raw = image_processed.getImage_processed_raw();
        if (image_raw == null) { return null; }
        return Imgcodecs.imdecode(
                new MatOfByte(image_raw),
                Imgcodecs.IMREAD_UNCHANGED
        );
    }

    public void write_image_raw(byte[] image_raw) {
        image_repository.save(new image(image_raw));
    }

    public void write_image(Mat image) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", image, buffer);
        byte[] image_raw = buffer.toArray();
        write_image_raw(image_raw);
    }

    public void write_image_processed_raw(
            byte[] image_processed_raw,
            Integer k_value,
            Double epsilon,
            segmentation_method segmentation_method,
            image original_image
    ) {
        image_processed_repository.save(new image_processed(
                image_processed_raw,
                k_value,
                epsilon,
                segmentation_method,
                original_image
        ));
    }

    public void write_image_processed(
            Mat image_processed,
            Integer k_value,
            Double epsilon,
            segmentation_method segmentation_method,
            image original_image
    ) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(images_extension, image_processed, buffer);
        byte[] image_processed_raw = buffer.toArray();
        write_image_processed_raw(image_processed_raw, k_value, epsilon, segmentation_method, original_image);
    }

    public void process_validation_images(String validation_images_base64_path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(validation_images_base64_path));
        String image_base64 = "";
        long i = 1;
        while ((image_base64 = reader.readLine()) != null) {
            byte[] file_content = Base64.getDecoder().decode(image_base64);
            if (read_by_id_image(i) == null) {
                write_image_raw(file_content);
            }
            i++;
        }

        int k_value = 3;
        double epsilon = 1;

        for (i = 1; i <= 6; i++) {
            image input_image = read_by_id_image(i);
            Mat input_image_mat = read_mat_by_image(input_image);

            Mat ordinary_k_means_plus_plus_output_image =
                    math_util.ordinary_k_means_plus_plus(input_image_mat, k_value, epsilon).output_image;
            write_image_processed(
                    ordinary_k_means_plus_plus_output_image,
                    k_value,
                    epsilon,
                    segmentation_method.ORDINARY_K_MEANS_PLUS_PLUS,
                    input_image
            );

            Mat constraints_k_medoids_plus_plus_output_image =
                    math_util.constraints_k_medoids_plus_plus(input_image_mat, k_value, epsilon).output_image;
            write_image_processed(
                    constraints_k_medoids_plus_plus_output_image,
                    k_value,
                    epsilon,
                    segmentation_method.CONSTRAINTS_K_MEDOIDS_PLUS_PLUS,
                    input_image
            );
        }
    }

    private final image_repository image_repository;

    private final image_processed_repository image_processed_repository;


    @GetMapping(
            value = "/home"
    )
    private String home(
        Model model
    ) {
        model.addAttribute("name", "bladway");
        return "home";
    }

    @GetMapping(
            value = "/auto_segmentation"
    )
    private String auto_segmentation_get(
            Model model
    ) {
        return "auto_segmentation";
    }

    @PostMapping(
            value = "/auto_segmentation"
    )
    private String auto_segmentation_post(
            Model model,
            @RequestParam("files") List<MultipartFile> files
    ) {
        System.out.println();
        return "auto_segmentation_result";
    }

    @GetMapping(
            value = "/manual_segmentation"
    )
    private String manual_segmentation(
            Model model
    ) {
        return "manual_segmentation";
    }


    /*@PostMapping(
            value = "/process",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
    )
    private String processKMeansRequest(
            @RequestPart MultipartFile image,
            @RequestPart int K
    ) {

        Object body;
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status;

        body = processWork(image, K);

        return

    }*/

}
