package ru.vsu.cs.bladway.controllers;

import lombok.RequiredArgsConstructor;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.vsu.cs.bladway.dtos.segmentation_result;
import ru.vsu.cs.bladway.enums.center_init_method;
import ru.vsu.cs.bladway.enums.segmentation_method;
import ru.vsu.cs.bladway.models.image;
import ru.vsu.cs.bladway.models.image_processed;
import ru.vsu.cs.bladway.repositories.image_processed_repository;
import ru.vsu.cs.bladway.repositories.image_repository;
import ru.vsu.cs.bladway.utils.math_util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static ru.vsu.cs.bladway.segmentation_app.images_extension;

@RequiredArgsConstructor
@Controller
public class segmentation_app_controller {

    public Mat read_image(image image) {
        if (image == null) return null;
        byte[] image_raw = image.getImage_raw();
        return Imgcodecs.imdecode(
                new MatOfByte(image_raw),
                Imgcodecs.IMREAD_UNCHANGED
        );
    }

    public Mat read_image(image_processed image_processed) {
        if (image_processed == null) return null;
        byte[] image_raw = image_processed.getImage_processed_raw();
        return Imgcodecs.imdecode(
                new MatOfByte(image_raw),
                Imgcodecs.IMREAD_UNCHANGED
        );
    }

    public Mat read_markup(image image) {
        if (image == null) return null;
        byte[] markup_raw = image.getMarkup_raw();
        Mat markup = Imgcodecs.imdecode(new MatOfByte(markup_raw), Imgcodecs.IMREAD_GRAYSCALE);
        Mat binary_markup = new Mat();
        Imgproc.threshold(markup, binary_markup, 127, 255, Imgproc.THRESH_BINARY);
        return binary_markup;
    }

    public void write_image(Mat image, Mat markup) {
        MatOfByte image_buffer = new MatOfByte();
        MatOfByte markup_buffer = new MatOfByte();
        Imgcodecs.imencode("." + images_extension, image, image_buffer);
        Imgcodecs.imencode("." + images_extension, markup, markup_buffer);
        image_repository.save(new image(image_buffer.toArray(), markup_buffer.toArray(), image.rows(), image.cols()));
    }

    public void write_image(BufferedImage image, BufferedImage markup) throws IOException {
        ByteArrayOutputStream image_stream = new ByteArrayOutputStream();
        ByteArrayOutputStream markup_stream = new ByteArrayOutputStream();
        ImageIO.write(image, images_extension, image_stream);
        ImageIO.write(markup, images_extension, markup_stream);
        image_repository.save(new image(
                image_stream.toByteArray(),
                markup_stream.toByteArray(),
                image.getHeight(),
                image.getWidth())
        );
    }

    public void write_image_processed(
            Mat image_processed,
            int k_value,
            int iteration_count,
            center_init_method center_init_method,
            segmentation_method segmentation_method,
            image original_image,
            List<Double> image_iteration_error_rates,
            List<Long> image_processing_times
    ) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode("." + images_extension, image_processed, buffer);
        byte[] image_processed_raw = buffer.toArray();
        image_processed_repository.save(new image_processed(
                image_processed_raw,
                k_value,
                iteration_count,
                center_init_method,
                segmentation_method,
                original_image,
                image_iteration_error_rates,
                image_processing_times
        ));
    }

    public void process_dataset(String dataset_path, int images_dataset_count) throws IOException {
        for (long i = 1; i <= images_dataset_count; i++) {
            if (image_repository.findById(i).orElse(null) == null) {
                write_image(
                        ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(
                                dataset_path + i + "." + images_extension))),
                        ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(
                                dataset_path + i + "_markup." + images_extension)))
                );
            }
        }

        //image_processed_repository.deleteAll();

        int iteration_count = 10;
        int passage_count = 3;
        int max_k = 4;

        for (long c = 1; c <= images_dataset_count; c++) {
            for (int p = 1; p <= passage_count; p++) {
                for (int k = 2; k <= max_k; k++) {
                    for (center_init_method ci_method : center_init_method.values()) {
                        for (segmentation_method seg_method : segmentation_method.values()) {
                            image input_image = image_repository.findById(c).orElse(null);
                            Mat input_image_mat = read_image(input_image);
                            Mat input_markup_mat = read_markup(input_image);
                            Mat output_image_mat;
                            segmentation_result output;
                            if (seg_method == segmentation_method.ORDINARY_K_MEANS) {
                                output = math_util.ordinary_k_means(
                                        input_image_mat, k, iteration_count, ci_method, input_markup_mat
                                );
                                // Рисуем отображение сегментов
                                output_image_mat = math_util.draw_segments(
                                        input_image_mat,
                                        output.centers,
                                        output.centers_labels,
                                        k
                                );
                            } else {
                                output = math_util.constraints_k_medoids(
                                        input_image_mat, k, iteration_count, ci_method, input_markup_mat
                                );
                                // Рисуем отображение сегментов
                                output_image_mat = math_util.draw_contours(
                                        input_image_mat,
                                        output.centers_labels,
                                        k
                                );
                            }

                            // Сохраняем результат сегментации
                            write_image_processed(
                                    output_image_mat,
                                    k,
                                    iteration_count,
                                    ci_method,
                                    seg_method,
                                    input_image,
                                    output.iteration_errors,
                                    output.processing_times
                            );
                        }
                    }
                }
            }
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
