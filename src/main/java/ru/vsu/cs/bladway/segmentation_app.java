package ru.vsu.cs.bladway;

import nu.pattern.OpenCV;
import org.opencv.core.Scalar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ru.vsu.cs.bladway.controllers.segmentation_app_controller;

import java.io.IOException;
import java.util.Random;

@SpringBootApplication(scanBasePackages={"ru.vsu.cs.bladway"})
@EntityScan("ru.vsu.cs.bladway.models")
@EnableJpaRepositories(basePackages = "ru.vsu.cs.bladway.repositories")
public class segmentation_app {

	public final static Random random = new Random(System.getenv("SEGMENTATION_APP_RANDOM_SEED").hashCode());

	public final static String images_extension = System.getenv("SEGMENTATION_APP_IMAGES_EXTENSION");

	public static Double center_window_size = 0.2;

	public static Scalar[] colors = new Scalar[] {
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
			new Scalar(255, 128, 0)
	};

	private final static String dataset_path = System.getenv("SEGMENTATION_APP_DATASET_PATH");

	private final static Integer images_dataset_count =
			Integer.valueOf(System.getenv("SEGMENTATION_APP_IMAGES_DATASET_COUNT"));

	public static void main(String[] args) throws IOException {
		OpenCV.loadLocally();

		//System.setProperty("java.awt.headless", "false");
		ApplicationContext context = SpringApplication.run(segmentation_app.class, args);
		context.getBean(segmentation_app_controller.class).process_dataset(dataset_path, images_dataset_count);
	}

}
