package ru.vsu.cs.bladway.controllers;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
import ru.vsu.cs.bladway.utils.chart_util;
import ru.vsu.cs.bladway.utils.math_util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static ru.vsu.cs.bladway.segmentation_app.*;

@RequiredArgsConstructor
@Controller
public class segmentation_app_controller {

    private final image_repository image_repository;
    private final image_processed_repository image_processed_repository;

    public Mat read_image(image image) {
        if (image == null) return null;
        byte[] image_raw = image.getImageRaw();
        return Imgcodecs.imdecode(
                new MatOfByte(image_raw),
                Imgcodecs.IMREAD_UNCHANGED
        );
    }

    public Mat read_image(image_processed image_processed) {
        if (image_processed == null) return null;
        byte[] image_raw = image_processed.getImageProcessedRaw();
        return Imgcodecs.imdecode(
                new MatOfByte(image_raw),
                Imgcodecs.IMREAD_UNCHANGED
        );
    }

    public Mat read_markup(image image) {
        if (image == null) return null;
        byte[] markup_raw = image.getMarkupRaw();
        Mat markup = Imgcodecs.imdecode(new MatOfByte(markup_raw), Imgcodecs.IMREAD_GRAYSCALE);
        Mat binary_markup = new Mat();
        Imgproc.threshold(markup, binary_markup, 127, 255, Imgproc.THRESH_BINARY);
        return binary_markup;
    }

    public image write_image(Mat image, Mat markup) {
        MatOfByte image_buffer = new MatOfByte();
        MatOfByte markup_buffer = new MatOfByte();
        Imgcodecs.imencode("." + images_extension, image, image_buffer);
        Imgcodecs.imencode("." + images_extension, markup, markup_buffer);
        return image_repository
                .save(new image(image_buffer.toArray(), markup_buffer.toArray(), image.rows(), image.cols()));
    }

    public image write_image(BufferedImage image, BufferedImage markup) throws IOException {
        ByteArrayOutputStream image_stream = new ByteArrayOutputStream();
        ByteArrayOutputStream markup_stream = new ByteArrayOutputStream();
        int height = 0;
        int width = 0;
        if (image != null) {
            ImageIO.write(image, images_extension, image_stream);
            height = image.getHeight();
            width = image.getWidth();
        }
        if (markup != null) {
            ImageIO.write(markup, images_extension, markup_stream);
        }
        return image_repository.save(new image(
                image_stream.toByteArray(),
                markup_stream.toByteArray(),
                height,
                width)
        );
    }

    public image_processed write_image_processed(
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
        return image_processed_repository.save(new image_processed(
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

    public void save_dataset(
            String dataset_path,
            long images_dataset
    ) throws IOException {
        for (long c = 1; c <= images_dataset; c++) {
            if (image_repository.findById(c).orElse(null) == null) {
                write_image(
                        ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(
                                dataset_path + c + "." + images_extension))),
                        ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(
                                dataset_path + c + "_markup." + images_extension)))
                );
            }
        }
    }

    public void process_dataset(
            int iteration_count,
            int passage_count,
            long images_dataset_min,
            long images_dataset_max,
            int k_min,
            int k_max
    ) throws IOException {
        for (int p = 1; p <= passage_count; p++) {
            for (long c = images_dataset_min; c <= images_dataset_max; c++) {
                for (int k = k_min; k <= k_max; k++) {
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

    @Transactional
    public void show_dataset_charts(
            int iteration_count,
            int passage_count,
            long images_dataset_min,
            long images_dataset_max,
            int k_min,
            int k_max
    ) throws IOException {
        for (long c = images_dataset_min; c <= images_dataset_max; c++) {
            for (int k = k_min; k <= k_max; k++) {
                Map<Pair<center_init_method, segmentation_method>, Double[]> error_data = new HashMap<>();
                Map<Pair<center_init_method, segmentation_method>, Double[]> time_data = new HashMap<>();
                for (center_init_method ci_method : center_init_method.values()) {
                    for (segmentation_method seg_method : segmentation_method.values()) {
                        List<image_processed> images_processed = image_processed_repository
                                .findAllBykValueAndOriginalImageAndCenterInitMethodAndSegmentationMethod(
                                        k,
                                        image_repository.findById(c).get(),
                                        ci_method,
                                        seg_method);
                        Double[] error_rates = new Double[iteration_count];
                        Arrays.fill(error_rates, 0.0);
                        Double[] times = new Double[iteration_count];
                        Arrays.fill(times, 0.0);
                        for (int i = 0; i < iteration_count; i++) {
                            for (int p = 0; p < passage_count; p++) {
                                error_rates[i] += images_processed.get(p).getImageIterationErrors().get(i);
                            }
                            error_rates[i] /= passage_count;
                        }
                        for (int i = 0; i < times.length; i++) {
                            for (int p = 0; p < passage_count; p++) {
                                times[i] += images_processed.get(p).getImageProcessingTimes().get(i);
                            }
                            times[i] /= passage_count;
                            times[i] /= 1000;
                        }
                        error_data.put(Pair.of(ci_method, seg_method), error_rates);
                        time_data.put(Pair.of(ci_method, seg_method), times);
                    }
                }
                String error_title = "Процесс обучения алгоритма. фото N = " + c + ". K = " + k;
                String time_title = "Общее время затраченное на число итераций. фото N = " + c + ". K = " + k;
                Mat error_chart_image = chart_util.get_iterations_errors_chart_image(error_data, error_title);
                Mat time_chart_image = chart_util.get_iterations_times_chart_image(time_data, time_title);
                chart_util.show_image(error_chart_image, error_title);
                chart_util.show_image(time_chart_image, time_title);
            }
        }
    }

    @GetMapping(
            value = "/home"
    )
    private String home_get(
            Model model
    ) {
        model.addAttribute("name", "bladway");
        return "home_get";
    }

    @GetMapping(
            value = "/auto_segmentation"
    )
    private String auto_segmentation_get(
            Model model
    ) {
        return "auto_segmentation_get";
    }

    @PostMapping(
            value = "/auto_segmentation"
    )
    private String auto_segmentation_post(
            Model model,
            @RequestParam("images") List<MultipartFile> images
    ) throws IOException {
        if (!dataset_saved) return "auto_segmentation_get";
        List<List<byte[]>> images_processed = new ArrayList<>();
        for (int m = 0; m < images.size(); m++) {
            images_processed.add(new ArrayList<>());
            image image = write_image(
                    ImageIO.read(Objects.requireNonNull(images.get(m).getInputStream())),
                    null
            );
            Mat image_mat = read_image(image);
            for (int k = k_min; k <= k_max; k++) {
                segmentation_result output = math_util.constraints_k_medoids(
                        image_mat, k, iteration_count, center_init_method.PAPER, null
                );
                // Рисуем отображение сегментов
                Mat output_image_mat = math_util.draw_contours(
                        image_mat,
                        output.centers_labels,
                        k
                );
                // Сохраняем результат сегментации
                image_processed image_processed = write_image_processed(
                        output_image_mat,
                        k,
                        iteration_count,
                        center_init_method.PAPER,
                        segmentation_method.CONSTRAINTS_K_MEDOIDS,
                        image,
                        output.iteration_errors,
                        output.processing_times
                );
                images_processed.get(m).add(image_processed.getImageProcessedRaw());
            }
        }
        model.addAttribute("images_processed", images_processed);
        return "auto_segmentation_post";
    }

    @GetMapping(
            value = "/manual_segmentation"
    )
    private String manual_segmentation(
            Model model
    ) {
        return "manual_segmentation_get";
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
