package ru.vsu.cs.bladway;

import nu.pattern.OpenCV;
import org.opencv.core.Scalar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import ru.vsu.cs.bladway.controllers.segmentation_app_controller;

import java.io.IOException;
import java.util.Random;

@SpringBootApplication
public class segmentation_app {

	public static String dataset_path = System.getenv("SEGMENTATION_APP_DATASET_PATH");

	public static Random random = new Random("segments".hashCode());

	public static String images_extension = ".png";

	public static Scalar[] colors = new Scalar[] {
			new Scalar(255, 0, 0),
			new Scalar(0, 255, 0),
			new Scalar(0, 0, 255),
			new Scalar(255, 255, 0),
			new Scalar(255, 0, 255),
			new Scalar(0, 255, 255),
			new Scalar(255, 128, 0),
			new Scalar(255, 0, 128),
			new Scalar(128, 255, 0),
			new Scalar(0, 255, 128),
			new Scalar(128, 0, 255),
			new Scalar(0, 128, 255),
	};

	public static void main(String[] args) throws IOException {
		OpenCV.loadLocally();

		//System.setProperty("java.awt.headless", "false");
		ApplicationContext context = SpringApplication.run(segmentation_app.class, args);
		context.getBean(segmentation_app_controller.class).process_dataset(dataset_path);
	}

}
