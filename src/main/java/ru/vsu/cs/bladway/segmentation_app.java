package ru.vsu.cs.bladway;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ru.vsu.cs.bladway.controllers.segmentation_app_controller;
import ru.vsu.cs.bladway.enums.center_init_method;
import ru.vsu.cs.bladway.enums.segmentation_method;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SpringBootApplication(scanBasePackages = {"ru.vsu.cs.bladway"})
@EntityScan("ru.vsu.cs.bladway.models")
@EnableJpaRepositories(basePackages = "ru.vsu.cs.bladway.repositories")
public class segmentation_app {

    public final static Random random = new Random(System.getenv("SEGMENTATION_APP_RANDOM_SEED").hashCode());
    public final static String images_extension = System.getenv("SEGMENTATION_APP_IMAGES_EXTENSION");
    public final static Double center_window_size = 0.2;
    public final static int iteration_count = 16;
    public final static int passage_count = 4;
    public final static long images_dataset = 6;
    public final static int images_dataset_min = 0;
    public final static int images_dataset_max = 5;
    public final static int k_min = 2;
    public final static int k_max = 5;
    public final static List<center_init_method> center_init_methods = List.of(
            center_init_method.RANDOM, center_init_method.PLUS_PLUS, center_init_method.PAPER
    );
    public final static List<segmentation_method> segmentation_methods = List.of(
            segmentation_method.ORDINARY_K_MEANS, segmentation_method.CONSTRAINTS_K_MEDOIDS
    );
    public final static Scalar[] colors = new Scalar[]{
            new Scalar(0, 0, 255),
            new Scalar(0, 255, 0),
            new Scalar(255, 0, 0),
            new Scalar(0, 255, 255),
            new Scalar(255, 0, 255),
            new Scalar(255, 255, 0),
            new Scalar(0, 128, 255),
            new Scalar(128, 0, 255),
            new Scalar(0, 255, 128),
            new Scalar(255, 0, 128),
            new Scalar(128, 255, 0),
            new Scalar(255, 128, 0),
            new Scalar(255, 128, 128),
            new Scalar(128, 255, 128),
            new Scalar(128, 128, 255),
            new Scalar(255, 128, 255),
            new Scalar(128, 255, 255),
            new Scalar(255, 255, 128),
            new Scalar(64, 64, 255),
            new Scalar(64, 255, 64),
            new Scalar(255, 64, 64),
            new Scalar(64, 255, 255),
            new Scalar(255, 64, 255),
            new Scalar(255, 255, 64)
    };
    private final static String dataset_path_in_resources =
            System.getenv("SEGMENTATION_APP_DATASET_PATH_IN_RESOURCES");
    private final static String charts_path = System.getenv("SEGMENTATION_APP_CHARTS_PATH");
    private final static String headless = System.getenv("SEGMENTATION_APP_HEADLESS");
    public static boolean dataset_written = false;
    public static boolean charts_written = false;

    public static void main(String[] args) throws IOException {
        OpenCV.loadLocally();
        System.setProperty("java.awt.headless", headless);
        segmentation_app_controller controller =
                SpringApplication.run(segmentation_app.class, args).getBean(segmentation_app_controller.class);

        var dataset = controller.write_dataset(dataset_path_in_resources, images_dataset);
        dataset_written = true;
        /*controller.process_dataset(
                dataset,
				iteration_count,
				passage_count,
				images_dataset_min,
				images_dataset_max,
				k_min,
				k_max,
                center_init_methods,
                segmentation_methods
		);*/
        var charts_mats = new ArrayList<Mat>();
        controller.add_charts_errors_by_iterations(
                dataset,
				iteration_count,
				passage_count,
				images_dataset_min,
				images_dataset_max,
				k_min,
				k_max,
                center_init_methods,
                segmentation_methods,
                charts_mats
		);
        controller.add_charts_errors_by_k(
                dataset,
                iteration_count,
                passage_count,
                images_dataset_min,
                images_dataset_max,
                k_min,
                k_max,
                center_init_methods,
                segmentation_methods,
                charts_mats
        );
        controller.add_charts_times_by_iterations(
                dataset,
                iteration_count,
                passage_count,
                images_dataset_min,
                images_dataset_max,
                k_min,
                k_max,
                center_init_methods,
                segmentation_methods,
                charts_mats
        );
        controller.add_charts_times_by_k(
                dataset,
                iteration_count,
                passage_count,
                images_dataset_min,
                images_dataset_max,
                k_min,
                k_max,
                center_init_methods,
                segmentation_methods,
                charts_mats
        );
        var charts = controller.write_charts(charts_mats);
        charts_written = true;
        if (!Boolean.parseBoolean(headless)) controller.show_charts(charts_mats);
        controller.save_charts(charts_path, charts);
    }

}
