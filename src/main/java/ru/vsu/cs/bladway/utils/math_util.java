package ru.vsu.cs.bladway.utils;

import lombok.RequiredArgsConstructor;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import ru.vsu.cs.bladway.dtos.pixel;
import ru.vsu.cs.bladway.dtos.queue_object;
import ru.vsu.cs.bladway.dtos.segmentation_result;
import ru.vsu.cs.bladway.enums.center_init_method;
import ru.vsu.cs.bladway.enums.center_update_method;

import java.util.*;

import static ru.vsu.cs.bladway.segmentation_app.*;

@RequiredArgsConstructor
public class math_util {

  /*public static Double findAverageDispersion(Mat startImage, Mat bestLabels, Mat centers, int K) {
        double[] sqrDistancesPerCluster = new double[K];
        long[] pixelsCountPerCluster = new long[K];
        for (int i = 0; i < startImage.rows(); i++) {
            for (int j = 0; j < startImage.cols(); j++) {
                int clusterNumber = (int) bestLabels.get(i, j)[0];
                sqrDistancesPerCluster[clusterNumber] +=
                        Math.pow(find_color_distance(
                                new Point3(centers.get(clusterNumber, 0)),
                                new Point3(startImage.get(i, j))
                        ), 2);
                pixelsCountPerCluster[clusterNumber]++;
            }
        }

        double[] dispersionsPerCluster = new double[K];
        for (int i = 0; i < dispersionsPerCluster.length; i++) {
            dispersionsPerCluster[i] = sqrDistancesPerCluster[i] / pixelsCountPerCluster[i];
        }
        return Arrays.stream(dispersionsPerCluster).sum() / dispersionsPerCluster.length;
    }*/

    public static Double find_color_distance(Point3 first_point, Point3 second_point) {
        return Math.sqrt(
                (second_point.x - first_point.x) * (second_point.x - first_point.x) +
                (second_point.y - first_point.y) * (second_point.y - first_point.y) +
                (second_point.z - first_point.z) * (second_point.z - first_point.z)
        );
    }

    public static Double find_color_distance(pixel first_pixel, pixel second_pixel) {
        return Math.sqrt(
                (second_pixel.b - first_pixel.b) * (second_pixel.b - first_pixel.b) +
                (second_pixel.g - first_pixel.g) * (second_pixel.g - first_pixel.g) +
                (second_pixel.r - first_pixel.r) * (second_pixel.r - first_pixel.r)
        );
    }

    public static Double find_color_distance(pixel first_pixel, Point3 second_point) {
        return Math.sqrt(
                (second_point.x - first_pixel.b) * (second_point.x - first_pixel.b) +
                (second_point.y - first_pixel.g) * (second_point.y - first_pixel.g) +
                (second_point.z - first_pixel.r) * (second_point.z - first_pixel.r)
        );
    }

    private static Mat swap_values(Mat mat, double y, double x) {
        // Маска первого элемента
        Mat y_binary_mask = new Mat();
        Core.compare(mat, Scalar.all(y), y_binary_mask, Core.CMP_EQ);
        // Маска второго элемента;
        Mat x_binary_mask = new Mat();
        Core.compare(mat, Scalar.all(x), x_binary_mask, Core.CMP_EQ);
        // Общая маска
        Mat y_or_x_binary_mask = new Mat();
        Core.bitwise_or(y_binary_mask, x_binary_mask, y_or_x_binary_mask);
        // Отрицание общей маски
        Mat not_y_or_x_binary_mask = new Mat();
        Core.bitwise_not(y_or_x_binary_mask, not_y_or_x_binary_mask);

        // Копируем второй элемент на позиции первого
        Mat y_to_x_values = new Mat();
        Core.multiply(y_binary_mask, Scalar.all(x), y_to_x_values);
        // Копируем первый элемент на позиции второго
        Mat x_to_y_values = new Mat();
        Core.multiply(x_binary_mask, Scalar.all(y), x_to_y_values);
        // Находим незатронутую матрицу значений
        Mat unchanged_values = new Mat();
        Core.copyTo(mat, unchanged_values, not_y_or_x_binary_mask);
        // Находим результирующую матрицу
        Mat result_values = new Mat();
        Core.add(y_to_x_values, x_to_y_values, result_values);
        Core.add(result_values, unchanged_values, result_values);
        return result_values;
    }

    private static List<pixel> pick_random_centers(Mat input_image, int K) {
        // Случайная инициализация центров
        List<pixel> centers = new ArrayList<>();
        for (int i = 0; i < K; i++) {
            // По умолчанию считаем, что новый добавляемый пиксель может совпадать с уже присутствующими
            int current_random_row = random.nextInt(input_image.rows());
            int current_random_col = random.nextInt(input_image.cols());
            // Проверяем, нет ли совпадения с уже существующими центрами по позиции
            for (pixel pixel : centers) {
                if (current_random_row == pixel.y && current_random_col == pixel.x) {
                    i--;
                    break;
                }
            }
            // Выполняем поиск i-го центра заново
            if (i == centers.size() - 1) continue;
            // Добавляем новый центр
            pixel center = new pixel(
                    current_random_row,
                    current_random_col,
                    input_image.get(current_random_row, current_random_col)
            );
            centers.add(center);
        }
        return centers;
    }

    private static List<pixel> pick_plus_plus_centers(Mat input_image, int K) {
        // Инициализация центров KMeans++ как самых удаленных друг от друга
        // со случайной первой
        List<pixel> centers = new ArrayList<>();
        boolean is_first_center = true;
        for (int k = 0; k < K; k++) {
            // Значение добавляемого центра
            pixel center;
            if (is_first_center) {
                // Первый центр в алгоритме выбирается случайным
                int y = random.nextInt(input_image.rows());
                int x = random.nextInt(input_image.cols());
                center = new pixel(y, x, input_image.get(y, x));
                is_first_center = false;
            } else {
                /* Нахождение пикселя, который находится на самом большом
                расстоянии от ближайшего центра из имеющихся */
                double max_global_distance_to_any_defined_center = -1;
                Point center_candidate = new Point();
                for (int y = 0; y < input_image.rows(); y++) {
                    for (int x = 0; x < input_image.cols(); x++) {
                        double min_distance_to_any_defined_center = Double.MAX_VALUE;
                        for (int l = 0; l < k; l++) {
                            double distance_to_current_centroid = find_color_distance(
                                    centers.get(l),
                                    new Point3(input_image.get(y, x))
                            );
                            if (distance_to_current_centroid < min_distance_to_any_defined_center) {
                                min_distance_to_any_defined_center = distance_to_current_centroid;
                            }
                        }
                        if (min_distance_to_any_defined_center > max_global_distance_to_any_defined_center) {
                            max_global_distance_to_any_defined_center = min_distance_to_any_defined_center;
                            center_candidate = new Point(x, y);
                        }
                    }
                }
                center = new pixel(
                    (int) center_candidate.y,
                    (int) center_candidate.x,
                    input_image.get((int) center_candidate.y, (int) center_candidate.x)
                );
            }
            // Добавляем новый центр
            centers.add(center);
        }
        return centers;
    }

    private static List<pixel> pick_paper_centers(Mat input_image, int K) {
        // Инициализация центров с помощью окна и угловых пикселей
        List<pixel> centers = new ArrayList<>();
        // Первым центром берем пиксель из центральной части изображения, наиболее похожий на существующие
        centers.add(find_new_center(
                input_image.submat(
                        (int) (input_image.rows() * (0.5 - center_window_size / 2)),
                        (int) (input_image.rows() * (0.5 + center_window_size / 2)),
                        (int) (input_image.cols() * (0.5 - center_window_size / 2)),
                        (int) (input_image.cols() * (0.5 + center_window_size / 2))
                ),
                new Mat(
                        (int) (input_image.rows() * center_window_size),
                        (int) (input_image.cols() * center_window_size),
                        CvType.CV_8UC1
                ).setTo(new Scalar(255)),
                center_update_method.medoid
        ));
        // Все остальные сегменты берем начиная с угловых частей изображения
        int div = (K - 1) / 4;
        int mod = (K - 1) % 4;
        if (div > 0) {
            centers.add(new pixel(0, 0, input_image));
            centers.add(new pixel(0, input_image.cols() - 1, input_image));
            centers.add(new pixel(input_image.rows() - 1, input_image.cols() - 1, input_image));
            centers.add(new pixel(input_image.rows() - 1, 0, input_image));
            // Сегменты, лежащие на гранях
            div--; int edge_segments;
            edge_segments = mod > 0 ? div + 1 : div;
            for (int i = 0; i < edge_segments; i++) {
                centers.add(new pixel(
                        0,
                        (input_image.cols() - 1) / (edge_segments + 1) * (i + 1),
                        input_image));
            }
            edge_segments = mod > 1 ? div + 1 : div;
            for (int i = 0; i < edge_segments; i++) {
                centers.add(new pixel(
                        (input_image.rows() - 1) / (edge_segments + 1) * (i + 1),
                        input_image.cols() - 1,
                        input_image));
            }
            edge_segments = mod > 2 ? div + 1 : div;
            for (int i = 0; i < edge_segments; i++) {
                centers.add(new pixel(
                        input_image.rows() - 1,
                        (input_image.cols() - 1) / (edge_segments + 1) * (i + 1),
                        input_image));
            }
            edge_segments = div;
            for (int i = 0; i < edge_segments; i++) {
                centers.add(new pixel(
                        (input_image.rows() - 1) / (edge_segments + 1) * (i + 1),
                        0,
                        input_image));
            }
        } else {
            if (mod > 0) centers.add(new pixel(0, 0, input_image));
            if (mod > 1) centers.add(new pixel(0, input_image.cols() - 1, input_image));
            if (mod > 2) centers.add(new pixel(input_image.rows() - 1, input_image.cols() - 1, input_image));
        }
        return centers;
    }

    private static void add_near_pixels_to_queue(Mat input_image, Queue<queue_object> queue, queue_object queue_object) {
        pixel pixel = queue_object.pixel;
        if (pixel.y > 0) {
            queue.add(new queue_object(
                    new pixel(pixel.y - 1, pixel.x, input_image),
                    queue_object.center_pixel,
                    queue_object.center_index
            ));
        }
        if (pixel.y < input_image.rows() - 1) {
            queue.add(new queue_object(
                    new pixel(pixel.y + 1, pixel.x, input_image),
                    queue_object.center_pixel,
                    queue_object.center_index
            ));
        }
        if (pixel.x > 0) {
            queue.add(new queue_object(
                    new pixel(pixel.y, pixel.x - 1, input_image),
                    queue_object.center_pixel,
                    queue_object.center_index
            ));
        }
        if (pixel.x < input_image.cols() - 1) {
            queue.add(new queue_object(
                    new pixel(pixel.y, pixel.x + 1, input_image),
                    queue_object.center_pixel,
                    queue_object.center_index
            ));
        }
    }

    private static void add_near_pixels_to_queue_modified(
            Mat input_image,
            Mat centers_labels,
            Queue<queue_object> queue,
            queue_object queue_object
    ) {
        pixel pixel = queue_object.pixel;
        if ((pixel.y > 0) && ((int) centers_labels.get(pixel.y - 1, pixel.x)[0] == -1)) {
            queue.add(new queue_object(
                    new pixel(pixel.y - 1, pixel.x, input_image),
                    queue_object.center_pixel,
                    queue_object.center_index
            ));
        }
        if ((pixel.y < input_image.rows() - 1) && ((int) centers_labels.get(pixel.y + 1, pixel.x)[0] == -1)) {
            queue.add(new queue_object(
                    new pixel(pixel.y + 1, pixel.x, input_image),
                    queue_object.center_pixel,
                    queue_object.center_index
            ));
        }
        if ((pixel.x > 0) && ((int) centers_labels.get(pixel.y, pixel.x - 1)[0] == -1)) {
            queue.add(new queue_object(
                    new pixel(pixel.y, pixel.x - 1, input_image),
                    queue_object.center_pixel,
                    queue_object.center_index
            ));
        }
        if ((pixel.x < input_image.cols() - 1) && ((int) centers_labels.get(pixel.y, pixel.x + 1)[0] == -1)) {
            queue.add(new queue_object(
                    new pixel(pixel.y, pixel.x + 1, input_image),
                    queue_object.center_pixel,
                    queue_object.center_index
            ));
        }
    }

    private static pixel find_new_center(Mat input_image, Mat binary_center_mask, center_update_method method) {
        Point3 sums_bgr = new Point3(0, 0, 0);
        long pixels_in_cluster = 0L;   // Количество пикселей в данном кластере
        // Проходим по каждому пикселю изображения и собираем информацию о кластере
        for (int y = 0; y < input_image.rows(); ++y) {
            for (int x = 0; x < input_image.cols(); ++x) {
                // Если пиксель принадлежит данному центру
                if ((int) binary_center_mask.get(y, x)[0] != 0) {
                    Point3 pixel_bgr = new Point3(input_image.get(y, x));
                    // Складываем данные по компонентам RGB
                    sums_bgr.x += pixel_bgr.x;
                    sums_bgr.y += pixel_bgr.y;
                    sums_bgr.z += pixel_bgr.z;
                    pixels_in_cluster++;
                }
            }
        }
        // Среднее значение цветов кластера
        Point3 mean = new Point3(
            sums_bgr.x / pixels_in_cluster,
            sums_bgr.y / pixels_in_cluster,
            sums_bgr.z / pixels_in_cluster
        );
        if (method == center_update_method.mean) {
            return new pixel(-1, -1, mean);
        }
        // Теперь найдем пиксель, наиболее близкий к этому среднему значению
        pixel medoid = null;
        double min_distance = Double.MAX_VALUE;
        // Находим пиксель с минимальным расстоянием
        for (int y = 0; y < input_image.rows(); ++y) {
            for (int x = 0; x < input_image.cols(); ++x) {
                // Если пиксель принадлежит данному кластеру
                if ((int) binary_center_mask.get(y, x)[0] != 0) {
                    Point3 pixel_bgr = new Point3(input_image.get(y, x));
                    double distance = find_color_distance(mean, pixel_bgr);
                    if (distance < min_distance) {
                        min_distance = distance;
                        medoid = new pixel(y, x, pixel_bgr);
                    }
                }
            }
        }
        return medoid;
    }

    public static segmentation_result ordinary_k_means(
        Mat input_image, int K, int iteration_count, center_init_method method
    ) {

        Mat centers_labels = new Mat(input_image.rows(), input_image.cols(), CvType.CV_8UC1);

        List<pixel> centers =
                method == center_init_method.RANDOM ? pick_random_centers(input_image, K) :
                method == center_init_method.PLUS_PLUS ? pick_plus_plus_centers(input_image, K) :
                method == center_init_method.PAPER ? pick_paper_centers(input_image, K) :
                pick_random_centers(input_image, K);

        // Выполняем заданное количество итераций
        for (int i = 0; i < iteration_count; i++) {
            /* Прикрепляем пиксели к ближайшим центрам
            и находим среднее значения для смещения туда центра */
            for (int y = 0; y < input_image.rows(); y++) {
                for (int x = 0; x < input_image.cols(); x++) {
                    double min_distance = Double.MAX_VALUE;
                    int minDistanceCenterIndex = -1;
                    for (int k = 0; k < centers.size(); k++) {
                        double distance = find_color_distance(
                                centers.get(k),
                                new Point3(input_image.get(y, x))
                        );
                        if (distance < min_distance) {
                            min_distance = distance;
                            minDistanceCenterIndex = k;
                        }
                    }
                    centers_labels.put(y, x, minDistanceCenterIndex);
                }
            }
            // Высчитываем новое положение каждого центра
            for (int k = 0; k < centers.size(); k++) {
                Mat binary_center_mask = new Mat();
                Core.compare(centers_labels, new Scalar(k), binary_center_mask, Core.CMP_EQ);
                pixel new_center = find_new_center(input_image, binary_center_mask, center_update_method.mean);
                centers.set(k, new_center);
            }
        }
        return new segmentation_result(centers_labels);
    }


    public static segmentation_result constraints_k_medoids(
            Mat input_image, int K, int iteration_count, center_init_method method
    ) {
        Mat centers_labels = new Mat(input_image.rows(), input_image.cols(), CvType.CV_8SC1);

        PriorityQueue<queue_object> queue = new PriorityQueue<>();

        List<pixel> centers =
                method == center_init_method.RANDOM ? pick_random_centers(input_image, K) :
                method == center_init_method.PLUS_PLUS ? pick_plus_plus_centers(input_image, K) :
                method == center_init_method.PAPER ? pick_paper_centers(input_image, K) :
                pick_random_centers(input_image, K);

        // Выполняем заданное число итераций
        for (int i = 0; i < iteration_count; i++) {
            // На новой итерации метки центров сбрасываются
            centers_labels.setTo(new Scalar(-1));
            // Добавляем центры в очередь с нулевым приоритетом
            for (int k = 0; k < centers.size(); k++) {
                queue.add(new queue_object(
                        centers.get(k), centers.get(k), k
                ));
            }
            // Запускаем очередь
            while (!queue.isEmpty()) {
                queue_object queue_object = queue.poll();
                pixel pixel = queue_object.pixel;
                // Проверка, что добавляемый пиксель еще не добавлен
                if ((int) centers_labels.get(pixel.y, pixel.x)[0] == -1) {
                    centers_labels.put(pixel.y, pixel.x, queue_object.center_index);
                    add_near_pixels_to_queue_modified(input_image, centers_labels, queue, queue_object);
                }
            }
            // Высчитываем новое положение каждого центра
            // Центр берется из кластера
            for (int k = 0; k < centers.size(); k++) {
                Mat binary_center_mask = new Mat();
                Core.compare(centers_labels, new Scalar(k), binary_center_mask, Core.CMP_EQ);
                pixel new_center = find_new_center(input_image, binary_center_mask, center_update_method.medoid);
                centers.set(k, new_center);
            }
        }
        // Когда все метки центров расставлены, переходим в только положительные значения
        centers_labels.convertTo(centers_labels, CvType.CV_8UC1);
        return new segmentation_result(centers_labels);
    }

    public static Mat make_paper_center_first(Mat centers_labels, int K) {
        int paper_cluster_index = -1;
        int paper_cluster_in_window_size = -1;
        Mat window_centers_labels = centers_labels.submat(
            (int) (centers_labels.rows() * (0.5 - center_window_size / 2)),
            (int) (centers_labels.rows() * (0.5 + center_window_size / 2)),
            (int) (centers_labels.cols() * (0.5 - center_window_size / 2)),
            (int) (centers_labels.cols() * (0.5 + center_window_size / 2))
        );
        for (int k = 0; k < K; k++) {
            Mat window_binary_center_mask = new Mat();
            Core.compare(window_centers_labels, new Scalar(k), window_binary_center_mask, Core.CMP_EQ);
            int cluster_in_window_size = (int) Core.sumElems(window_binary_center_mask).val[0];
            if (cluster_in_window_size > paper_cluster_in_window_size) {
                paper_cluster_index = k;
                paper_cluster_in_window_size = cluster_in_window_size;
            }
        }
        return swap_values(centers_labels, 0, paper_cluster_index);
    }

    public static Mat draw_contours(Mat input_image, Mat centers_labels, int K) {
        Mat output_image = input_image.clone();
        for (int k = 0; k < K; k++) {
            // Бинаризационная маска текущего сегмента
            Mat binary_center_mask = new Mat();
            Core.compare(centers_labels, new Scalar(k), binary_center_mask, Core.CMP_EQ);
            // Осуществляем отсуп внутри каждого сегмента, чтобы границы были более явными
            Mat erode_shape = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6));
            Mat erode_binary_center_mask = new Mat();
            Imgproc.erode(binary_center_mask, erode_binary_center_mask, erode_shape);
            // Нахождение контуров текущего сегмента
            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.findContours(erode_binary_center_mask, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
            // Отрисовка всех контуров кластера цветом по очереди
            Imgproc.drawContours(
                    output_image,
                    contours,
                    -1,
                    colors[k],
                    8
            );
        }
        return output_image;
    }

    public static Mat draw_segments(Mat input_image, List<pixel> centers, Mat centers_labels, int K) {
        Mat output_image = input_image.clone();
        for (int k = 0; k < K; k++) {
            // Бинаризационная маска текущего сегмента
            Mat binary_center_mask = new Mat();
            Core.compare(centers_labels, new Scalar(k), binary_center_mask, Core.CMP_EQ);
            output_image.setTo(new Scalar(centers.get(k).b, centers.get(k).g, centers.get(k).r), binary_center_mask);
        }
        return output_image;
    }

}
