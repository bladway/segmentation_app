package ru.vsu.cs.bladway.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.vsu.cs.bladway.dtos.pixel;
import ru.vsu.cs.bladway.dtos.point;
import ru.vsu.cs.bladway.dtos.segmentation_result;
import ru.vsu.cs.bladway.enums.center_init_method;
import ru.vsu.cs.bladway.enums.segmentation_method;
import ru.vsu.cs.bladway.models.image;
import ru.vsu.cs.bladway.models.image_processed;
import ru.vsu.cs.bladway.repositories.image_processed_repository;
import ru.vsu.cs.bladway.repositories.image_repository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static ru.vsu.cs.bladway.segmentation_app.*;
import static ru.vsu.cs.bladway.utils.chart_util.get_ci_seg_methods_xy_chart_image;
import static ru.vsu.cs.bladway.utils.chart_util.show_image;
import static ru.vsu.cs.bladway.utils.math_util.*;

@RequiredArgsConstructor
@Controller
public class segmentation_app_controller {

    private final image_repository image_repository;
    private final image_processed_repository image_processed_repository;

    public Mat read_image(image image) {
        if (image == null) return null;
        byte[] image_raw = image.getImage();
        return Imgcodecs.imdecode(
                new MatOfByte(image_raw),
                Imgcodecs.IMREAD_UNCHANGED
        );
    }

    public Mat read_image_markup(image image) {
        if (image == null) return null;
        byte[] markup_raw = image.getImageMarkup();
        Mat markup = Imgcodecs.imdecode(new MatOfByte(markup_raw), Imgcodecs.IMREAD_GRAYSCALE);
        Mat binary_markup = new Mat();
        Imgproc.threshold(markup, binary_markup, 127, 255, Imgproc.THRESH_BINARY);
        return binary_markup;
    }

    public Mat read_image(image_processed image_processed) {
        if (image_processed == null) return null;
        byte[] image_byte = image_processed.getImageProcessedSegments();
        return Imgcodecs.imdecode(
                new MatOfByte(image_byte),
                Imgcodecs.IMREAD_UNCHANGED
        );
    }

    public Mat read_image_labels(image_processed image_processed) {
        if (image_processed == null) return null;
        byte[] image_byte = image_processed.getImageProcessedLabels();
        return Imgcodecs.imdecode(new MatOfByte(image_byte), Imgcodecs.IMREAD_GRAYSCALE);
    }

    public byte[] read_image(Mat image) {
        if (image == null) return null;
        MatOfByte image_buffer = new MatOfByte();
        Imgcodecs.imencode("." + images_extension, image, image_buffer);
        return image_buffer.toArray();
    }

    public byte[] read_image(BufferedImage image) throws IOException {
        if (image == null) return null;
        ByteArrayOutputStream image_stream = new ByteArrayOutputStream();
        ImageIO.write(image, images_extension, image_stream);
        return image_stream.toByteArray();
    }

    public image write_image(Mat image, Mat image_markup) {
        byte[] image_byte = read_image(image);
        byte[] image_markup_byte = read_image(image_markup);
        return image_repository.save(new image(image_byte, image_markup_byte, image.rows(), image.cols()));
    }

    public image write_image(BufferedImage image, BufferedImage image_markup) throws IOException {
        byte[] image_byte = read_image(image);
        byte[] image_markup_byte = read_image(image_markup);
        return image_repository.save(new image(image_byte, image_markup_byte, image.getHeight(), image.getWidth()));
    }

    public image_processed write_image_processed(
            Mat centers_labels,
            Mat image_processed,
            int k_value,
            int iteration_count,
            center_init_method center_init_method,
            segmentation_method segmentation_method,
            image original_image,
            List<Double> image_iteration_error_rates,
            List<Long> image_processing_times
    ) {
        byte[] image_processed_centers_byte = read_image(centers_labels);
        byte[] image_processed_segments_byte = read_image(image_processed);
        return image_processed_repository.save(new image_processed(
                image_processed_centers_byte,
                image_processed_segments_byte,
                k_value,
                iteration_count,
                center_init_method,
                segmentation_method,
                original_image,
                image_iteration_error_rates,
                image_processing_times
        ));
    }

    public image_processed write_image_processed(
            BufferedImage centers_labels,
            BufferedImage image_processed,
            int k_value,
            int iteration_count,
            center_init_method center_init_method,
            segmentation_method segmentation_method,
            image original_image,
            List<Double> image_iteration_error_rates,
            List<Long> image_processing_times
    ) throws IOException {
        byte[] image_processed_centers_byte = read_image(centers_labels);
        byte[] image_processed_segments_byte = read_image(image_processed);
        return image_processed_repository.save(new image_processed(
                image_processed_centers_byte,
                image_processed_segments_byte,
                k_value,
                iteration_count,
                center_init_method,
                segmentation_method,
                original_image,
                image_iteration_error_rates,
                image_processing_times
        ));
    }

    public List<image> save_dataset(
            String dataset_path,
            long images_dataset
    ) throws IOException {
        List<image> images = new ArrayList<>();
        for (long c = 1; c <= images_dataset; c++) {
            image image = image_repository.findById(c).orElse(null);
            if (image == null) {
                images.add(write_image(
                        ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(
                                dataset_path + c + "." + images_extension))),
                        ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(
                                dataset_path + c + "_markup." + images_extension)))
                ));
            } else images.add(image);
        }
        return images;
    }

    public void process_dataset(
            List<image> dataset,
            int iteration_count,
            int passage_count,
            int images_dataset_min,
            int images_dataset_max,
            int k_min,
            int k_max,
            List<center_init_method> center_init_methods,
            List<segmentation_method> segmentation_methods
    ) {
        for (int p = 1; p <= passage_count; p++) {
            for (int c = images_dataset_min; c <= images_dataset_max; c++) {
                for (int k = k_min; k <= k_max; k++) {
                    for (center_init_method ci_method : center_init_methods) {
                        for (segmentation_method seg_method : segmentation_methods) {
                            image input_image = dataset.get(c);
                            Mat input_image_mat = read_image(input_image);
                            Mat input_markup_mat = read_image_markup(input_image);
                            Mat output_image_mat;
                            segmentation_result output;
                            if (seg_method == segmentation_method.ORDINARY_K_MEANS) {
                                output = ordinary_k_means(
                                        input_image_mat,
                                        k,
                                        iteration_count,
                                        ci_method,
                                        input_markup_mat,
                                        null
                                );
                                // Рисуем отображение сегментов
                                output_image_mat = draw_segments(
                                        input_image_mat,
                                        output.centers,
                                        output.centers_labels,
                                        k
                                );
                            } else if (seg_method == segmentation_method.CONSTRAINTS_K_MEDOIDS) {
                                output = constraints_k_medoids(
                                        input_image_mat,
                                        k,
                                        iteration_count,
                                        ci_method,
                                        input_markup_mat,
                                        null
                                );
                                // Рисуем отображение сегментов
                                output_image_mat = draw_contours(
                                        input_image_mat,
                                        output.centers_labels,
                                        k
                                );
                            } else continue;

                            // Сохраняем результат сегментации
                            write_image_processed(
                                    output.centers_labels,
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

    public void show_charts_errors_by_iterations(
            List<image> dataset,
            int iteration_count,
            int passage_count,
            int images_dataset_min,
            int images_dataset_max,
            int k_min,
            int k_max,
            List<center_init_method> center_init_methods,
            List<segmentation_method> segmentation_methods
    ) throws IOException {
        for (int c = images_dataset_min; c <= images_dataset_max; c++) {
            for (int k = k_min; k <= k_max; k++) {
                image original_image = dataset.get(c);
                Map<Pair<center_init_method, segmentation_method>, Double[][]> error_data = new HashMap<>();
                for (center_init_method ci_method : center_init_methods) {
                    for (segmentation_method seg_method : segmentation_methods) {
                        Double[][] error_rates = new Double[2][iteration_count];
                        Arrays.fill(error_rates[1], 0.0);
                        List<image_processed> images_processed = image_processed_repository
                                .findAllBykValueAndOriginalImageAndCenterInitMethodAndSegmentationMethod(
                                        k,
                                        original_image,
                                        ci_method,
                                        seg_method);
                        for (int i = 0; i < iteration_count; i++) {
                            error_rates[0][i] = (double) (i + 1);
                            for (int p = 0; p < passage_count; p++) {
                                error_rates[1][i] += images_processed.get(p).getImageIterationErrors().get(i);
                            }
                            error_rates[1][i] /= passage_count;
                        }
                        error_data.put(Pair.of(ci_method, seg_method), error_rates);
                    }
                }
                String error_title = "Процесс обучения алгоритма." +
                        " Фото N = " + c
                        + ". K = " + k;
                Mat error_chart_image = get_ci_seg_methods_xy_chart_image(error_data,
                        error_title,
                        "Итерация алгоритма",
                        "Величина ошибки"
                );
                show_image(error_chart_image, error_title);
            }
        }
    }

    public void show_charts_errors_by_k(
            List<image> dataset,
            int iteration_count,
            int passage_count,
            int images_dataset_min,
            int images_dataset_max,
            int k_min,
            int k_max,
            List<center_init_method> center_init_methods,
            List<segmentation_method> segmentation_methods
    ) throws IOException {
        for (int c = images_dataset_min; c <= images_dataset_max; c++) {
            image original_image = dataset.get(c);
            Map<Pair<center_init_method, segmentation_method>, Double[][]> error_data = new HashMap<>();
            for (center_init_method ci_method : center_init_methods) {
                for (segmentation_method seg_method : segmentation_methods) {
                    Double[][] error_rates = new Double[2][k_max - k_min + 1];
                    Arrays.fill(error_rates[1], 0.0);
                    for (int k = k_min; k <= k_max; k++) {
                        error_rates[0][k - k_min] = (double) k;
                        List<image_processed> images_processed = image_processed_repository
                                .findAllBykValueAndOriginalImageAndCenterInitMethodAndSegmentationMethod(
                                        k,
                                        original_image,
                                        ci_method,
                                        seg_method);
                        for (int p = 0; p < passage_count; p++) {
                            error_rates[1][k - k_min] += images_processed.get(p).getImageIterationErrors().getLast();
                        }
                        error_rates[1][k - k_min] /= passage_count;
                    }
                    error_data.put(Pair.of(ci_method, seg_method), error_rates);
                }
            }
            String error_title = "Величина ошибки при разных значениях числа сегментов." +
                    " Фото N = " + c
                    + ". Количество итераций = " + iteration_count;
            Mat error_chart_image = get_ci_seg_methods_xy_chart_image(
                    error_data,
                    error_title,
                    "Величина K",
                    "Величина ошибки"
            );
            show_image(error_chart_image, error_title);
        }
    }

    public void show_charts_times_by_iterations(
            List<image> dataset,
            int iteration_count,
            int passage_count,
            int images_dataset_min,
            int images_dataset_max,
            int k_min,
            int k_max,
            List<center_init_method> center_init_methods,
            List<segmentation_method> segmentation_methods
    ) throws IOException {
        for (int c = images_dataset_min; c <= images_dataset_max; c++) {
            for (int k = k_min; k <= k_max; k++) {
                image original_image = dataset.get(c);
                Map<Pair<center_init_method, segmentation_method>, Double[][]> time_data = new HashMap<>();
                for (center_init_method ci_method : center_init_methods) {
                    for (segmentation_method seg_method : segmentation_methods) {
                        Double[][] times = new Double[2][iteration_count];
                        Arrays.fill(times[1], 0.0);
                        List<image_processed> images_processed = image_processed_repository
                                .findAllBykValueAndOriginalImageAndCenterInitMethodAndSegmentationMethod(
                                        k,
                                        original_image,
                                        ci_method,
                                        seg_method);
                        for (int i = 0; i < times.length; i++) {
                            times[0][i] = (double) (i + 1);
                            for (int p = 0; p < passage_count; p++) {
                                times[1][i] += images_processed.get(p).getImageProcessingTimes().get(i);
                            }
                            times[1][i] /= passage_count;
                            times[1][i] /= 1000;
                        }
                        time_data.put(Pair.of(ci_method, seg_method), times);
                    }
                }
                String time_title = "Общее время затраченное на число итераций." +
                        " Фото N = " + c
                        + ", высота = " + original_image.getImageHeight()
                        + ", ширина = " + original_image.getImageWidth()
                        + ". K = " + k;
                Mat time_chart_image = get_ci_seg_methods_xy_chart_image(
                        time_data,
                        time_title,
                        "Итерация алгоритма",
                        "Время в секундах"
                );
                show_image(time_chart_image, time_title);
            }
        }
    }

    public void show_charts_times_by_k(
            List<image> dataset,
            int iteration_count,
            int passage_count,
            int images_dataset_min,
            int images_dataset_max,
            int k_min,
            int k_max,
            List<center_init_method> center_init_methods,
            List<segmentation_method> segmentation_methods
    ) throws IOException {
        for (int c = images_dataset_min; c <= images_dataset_max; c++) {
            image original_image = dataset.get(c);
            Map<Pair<center_init_method, segmentation_method>, Double[][]> time_data = new HashMap<>();
            for (center_init_method ci_method : center_init_methods) {
                for (segmentation_method seg_method : segmentation_methods) {
                    Double[][] times = new Double[2][k_max - k_min + 1];
                    Arrays.fill(times[1], 0.0);
                    for (int k = k_min; k <= k_max; k++) {
                        times[0][k - k_min] = (double) k;
                        List<image_processed> images_processed = image_processed_repository
                                .findAllBykValueAndOriginalImageAndCenterInitMethodAndSegmentationMethod(
                                        k,
                                        original_image,
                                        ci_method,
                                        seg_method);
                        for (int p = 0; p < passage_count; p++) {
                            times[1][k - k_min] += images_processed.get(p).getImageProcessingTimes().getLast();
                        }
                        times[1][k - k_min] /= passage_count;
                        times[1][k - k_min] /= 1000;
                    }
                    time_data.put(Pair.of(ci_method, seg_method), times);
                }
            }
            String time_title = "Общее время работы при разных значениях числа сегментов." +
                    " Фото N = " + c
                    + ", высота = " + original_image.getImageHeight()
                    + ", ширина = " + original_image.getImageWidth()
                    + ". Количество итераций = " + iteration_count;
            Mat time_chart_image = get_ci_seg_methods_xy_chart_image(
                    time_data,
                    time_title,
                    "Величина K",
                    "Время в секундах"
            );
            show_image(time_chart_image, time_title);
        }
    }

    @GetMapping(
            value = "/home"
    )
    private String home_get(
            Model model
    ) {
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
        List<List<String>> images_processed_base64 = new ArrayList<>();
        for (int m = 0; m < images.size(); m++) {
            images_processed_base64.add(new ArrayList<>());
            image image_db = write_image(
                    ImageIO.read(Objects.requireNonNull(images.get(m).getInputStream())),
                    null
            );
            Mat image_mat = read_image(image_db);
            for (int k = k_min; k <= k_max; k++) {
                segmentation_result output = constraints_k_medoids(
                        image_mat, k, iteration_count, center_init_method.PAPER, null, null
                );
                // Рисуем отображение сегментов
                Mat output_image_mat = draw_contours(
                        image_mat,
                        output.centers_labels,
                        k
                );
                // Сохраняем результат сегментации
                write_image_processed(
                        output.centers_labels,
                        output_image_mat,
                        k,
                        iteration_count,
                        center_init_method.PAPER,
                        segmentation_method.CONSTRAINTS_K_MEDOIDS,
                        image_db,
                        output.iteration_errors,
                        output.processing_times
                );
                images_processed_base64.get(m).add(
                        Base64.getEncoder()
                                .encodeToString(read_image(cut_edges(image_mat, output.centers_labels, k))));
            }
        }
        model.addAttribute("images_processed_base64", images_processed_base64);
        return "auto_segmentation_post";
    }

    @GetMapping(
            value = "/manual_segmentation"
    )
    private String manual_segmentation_get(
            Model model
    ) {
        return "manual_segmentation_get";
    }

    @PostMapping(
            value = "/manual_segmentation",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    private String manual_segmentation_post(
            Model model,
            @RequestParam("image") MultipartFile image,
            @RequestParam("points") String pointsJSON
    ) throws IOException {
        if (!dataset_saved) return "manual_segmentation_get";
        ObjectMapper mapper = new ObjectMapper();
        List<point> points = mapper.readValue(pointsJSON, new TypeReference<List<point>>() {
        });
        String image_processed_base64;
        image image_db = write_image(
                ImageIO.read(Objects.requireNonNull(image.getInputStream())),
                null
        );
        Mat image_mat = read_image(image_db);
        List<pixel> set_centers = new ArrayList<>();
        // Добавляем центры обрезаемых сегментов
        points.forEach(point -> set_centers.add(new pixel(point.y, point.x, image_mat.get(point.y, point.x))));
        // Добавляем центр сегмента бумаги
        set_centers.add(pick_window_center(image_mat));
        segmentation_result output = constraints_k_medoids(
                image_mat, set_centers.size(), iteration_count, center_init_method.SET, null, set_centers
        );
        // Рисуем отображение сегментов
        Mat output_image_mat = draw_contours(
                image_mat,
                output.centers_labels,
                set_centers.size()
        );
        // Сохраняем результат сегментации
        write_image_processed(
                output.centers_labels,
                output_image_mat,
                set_centers.size(),
                iteration_count,
                center_init_method.SET,
                segmentation_method.CONSTRAINTS_K_MEDOIDS,
                image_db,
                output.iteration_errors,
                output.processing_times
        );
        image_processed_base64 = Base64.getEncoder().encodeToString(
                read_image(cut_edges(image_mat, output.centers_labels, set_centers.size())));
        model.addAttribute("image_processed_base64", image_processed_base64);
        return "manual_segmentation_post";
    }


}
